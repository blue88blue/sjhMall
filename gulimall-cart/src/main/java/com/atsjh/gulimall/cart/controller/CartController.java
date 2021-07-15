package com.atsjh.gulimall.cart.controller;

import com.atsjh.gulimall.cart.interceptor.CartInteceptor;
import com.atsjh.gulimall.cart.service.CartService;
import com.atsjh.gulimall.cart.to.UserInfoTo;
import com.atsjh.gulimall.cart.vo.CartItemVo;
import com.atsjh.gulimall.cart.vo.CartVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author: sjh
 * @date: 2021/7/8 下午4:13
 * @description:
 */
@Controller
public class CartController {

    @Autowired
    CartService cartService;

    @GetMapping("/getCheckedItems")
    @ResponseBody
    public  List<CartItemVo> getCheckedItems(){
        return cartService.getCheckedItems();
    }


    @GetMapping("/getItemsForOrder")
    @ResponseBody
    public List<CartItemVo> getItemsForOrder(){
        return cartService.getItemsForOrder();
    }


    /**
     * 商品是否选中
     *
     * @param skuId
     * @param checked
     * @return
     */
    @GetMapping(value = "/checkItem")
    public String checkItem(@RequestParam(value = "skuId") Long skuId,
                            @RequestParam(value = "checked") Integer checked) {

        cartService.checkItem(skuId, checked);

        return "redirect:http://cart.gulimall.com/cart.html";

    }


    /**
     * 改变商品数量
     *
     * @param skuId
     * @param num
     * @return
     */
    @GetMapping(value = "/countItem")
    public String countItem(@RequestParam(value = "skuId") Long skuId,
                            @RequestParam(value = "num") Integer num) {

        cartService.changeItemCount(skuId, num);

        return "redirect:http://cart.gulimall.com/cart.html";
    }


    /**
     * 删除商品信息
     *
     * @param skuId
     * @return
     */
    @GetMapping(value = "/deleteItem")
    public String deleteItem(@RequestParam("skuId") Integer skuId) {

        cartService.deleteIdCartInfo(skuId);

        return "redirect:http://cart.gulimall.com/cart.html";

    }


    @GetMapping("cart.html")
    public String getCart(Model model){
        CartVo cart = cartService.getCart();
        model.addAttribute("cart", cart);
        return "cartList";
    }

    @GetMapping("addCartItem")
    public String addVartItem(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num,
                              RedirectAttributes redirectAttributes) throws ExecutionException, InterruptedException {
        CartItemVo cartItem = cartService.addCartItem(skuId, num);
        redirectAttributes.addAttribute("skuId", skuId);
        return "redirect:http://cart.gulimall.com/addCartItemSuccess.html";
    }


    @GetMapping("addCartItemSuccess.html")
    public String success(@RequestParam("skuId") Long skuId, Model model){
        CartItemVo cartItem = cartService.getCartItem(skuId);
        model.addAttribute("cartItem", cartItem);
        return "success";
    }
}
