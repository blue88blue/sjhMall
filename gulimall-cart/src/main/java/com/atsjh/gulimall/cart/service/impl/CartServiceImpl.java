package com.atsjh.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atsjh.common.constant.CartConstant;
import com.atsjh.common.utils.R;
import com.atsjh.gulimall.cart.feign.ProductFeignService;
import com.atsjh.gulimall.cart.interceptor.CartInteceptor;
import com.atsjh.gulimall.cart.service.CartService;
import com.atsjh.gulimall.cart.to.UserInfoTo;
import com.atsjh.gulimall.cart.vo.CartItemVo;
import com.atsjh.gulimall.cart.vo.CartVo;
import com.atsjh.gulimall.cart.vo.SkuInfoVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author: sjh
 * @date: 2021/7/8 下午5:12
 * @description:
 */
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Override
    public CartItemVo addCartItem(Long skuId, Integer num) throws ExecutionException, InterruptedException {

        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        //获取redis中该用户的购物车中该sku商品的信息, 检查购物车中是否有该商品
        String productRedisValue = (String)cartOps.get(skuId.toString()); //json字符串

        //添加item数据
        if(StringUtils.isEmpty(productRedisValue)){
            //向购物车中增加商品，
            CartItemVo cartItemVo = new CartItemVo();
            cartItemVo.setSkuId(skuId);
            cartItemVo.setCount(num);
            //调用远程服务查询图片标题和价格
            CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                R r = productFeignService.info(skuId);
                if(r.getCode() == 0){
                    String jsonString = JSON.toJSONString(r.get("skuInfo"));
                    SkuInfoVo skuInfoVo = JSON.parseObject(jsonString, new TypeReference<SkuInfoVo>() {
                    });
                    cartItemVo.setImage(skuInfoVo.getSkuDefaultImg());
                    cartItemVo.setPrice(skuInfoVo.getPrice());
                    cartItemVo.setTitle(skuInfoVo.getSkuTitle());
                }
            });
            // openfeign查询sku的销售属性
            CompletableFuture<Void> getSaleAttrsTask = CompletableFuture.runAsync(() -> {
                List<String> skuSaleAttrValues = productFeignService.getSkuSaleAttrValues(skuId);
                cartItemVo.setSkuAttrValues(skuSaleAttrValues);
            });
            CompletableFuture.allOf(getSaleAttrsTask, getSkuInfoTask).get();

            //保存到redis
            String jsonString = JSON.toJSONString(cartItemVo);
            cartOps.put(skuId.toString(), jsonString);
            return cartItemVo;
        }
        else{
            //增加sku数量
            CartItemVo cartItemVo = JSON.parseObject(productRedisValue, new TypeReference<CartItemVo>() {
            });
            cartItemVo.setCount(cartItemVo.getCount() + num);
            //保存到redis
            String jsonString = JSON.toJSONString(cartItemVo);
            cartOps.put(skuId.toString(), jsonString);
            return cartItemVo;
        }
    }

    @Override
    public CartItemVo getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        String s = (String) cartOps.get(skuId.toString());
        CartItemVo cartItemVo = JSON.parseObject(s, new TypeReference<CartItemVo>() {
        });
        return cartItemVo;
    }

    @Override
    public CartVo getCart() {
        UserInfoTo userInfoTo = CartInteceptor.toThreadLocal.get();
        //有没有登录
        if(userInfoTo.getUserId() != null){
            //登录了要合并购物车
            // 获取临时购物车
            String tempCartKey = CartConstant.CART_PREFIX + userInfoTo.getUserKey();
            CartVo tempCart = getCartByKey(tempCartKey);
            //将每个购物项加入登录购物车
            if(tempCart.getItems() != null){
                tempCart.getItems().forEach(cartItemVo -> {
                    try {
                        addCartItem(cartItemVo.getSkuId(), cartItemVo.getCount());
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }
            //清空临时购物车
            redisTemplate.delete(tempCartKey);

            //获取登录购物车
            CartVo cart = getCartByKey(CartConstant.CART_PREFIX + userInfoTo.getUserId());
            return cart;
        }
        else{
            //没登录
            String tempCartKey = CartConstant.CART_PREFIX + userInfoTo.getUserKey();
            CartVo tempCart = getCartByKey(tempCartKey);
            return tempCart;
        }
    }

    @Override
    public void checkItem(Long skuId, Integer checked) {
        CartItemVo cartItem = getCartItem(skuId);
        cartItem.setCheck(checked == 1);

        String jsonString = JSON.toJSONString(cartItem);
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(), jsonString);
    }

    @Override
    public void changeItemCount(Long skuId, Integer num) {
        //查询购物车里面的商品
        CartItemVo cartItem = getCartItem(skuId);
        cartItem.setCount(num);

        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        //序列化存入redis中
        String redisValue = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(), redisValue);
    }

    @Override
    public void deleteIdCartInfo(Integer skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }

    public CartVo getCartByKey(String tempCartKey){
        CartVo cartVo = new CartVo();
        BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(tempCartKey);
        List<Object> values = ops.values();
        ArrayList<CartItemVo> cartItemVos = new ArrayList<>();
        if(values != null && values.size()>1){
            for(Object o : values){
                String jsonString = (String) o;
                CartItemVo cartItemVo = JSON.parseObject(jsonString, CartItemVo.class);
                cartItemVos.add(cartItemVo);
            }
            cartVo.setItems(cartItemVos);
        }
        return cartVo;
    }


    /**
     * 返回要操作的购物车操作
     * @return
     */
    private BoundHashOperations<String, Object, Object> getCartOps(){
        //检查用户是否登录， 将购物车信息存入redis中的不同字段
        UserInfoTo userInfoTo = CartInteceptor.toThreadLocal.get();
        String key = "";
        if(userInfoTo.getUserId() == null){
            key = CartConstant.CART_PREFIX + userInfoTo.getUserKey();
        }
        else{
            key = CartConstant.CART_PREFIX + userInfoTo.getUserId();
        }
        BoundHashOperations<String, Object, Object> stringObjectObjectBoundHashOperations = redisTemplate.boundHashOps(key);
        return stringObjectObjectBoundHashOperations;
    }
}
