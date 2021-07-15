package com.atsjh.gulimall.cart.service;

import com.atsjh.gulimall.cart.vo.CartItemVo;
import com.atsjh.gulimall.cart.vo.CartVo;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author: sjh
 * @date: 2021/7/8 下午5:12
 * @description:
 */
public interface CartService {
    CartItemVo addCartItem(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    CartItemVo getCartItem(Long skuId);

    CartVo getCart();

    void checkItem(Long skuId, Integer checked);

    void changeItemCount(Long skuId, Integer num);

    void deleteIdCartInfo(Integer skuId);

    List<CartItemVo> getItemsForOrder();

    List<CartItemVo> getCheckedItems();

}
