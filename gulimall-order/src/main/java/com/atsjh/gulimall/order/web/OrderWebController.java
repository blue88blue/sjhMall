package com.atsjh.gulimall.order.web;

import com.atsjh.gulimall.order.service.OrderService;
import com.atsjh.gulimall.order.vo.OrderConfirmVo;
import com.atsjh.gulimall.order.vo.OrderSubmitVo;
import com.atsjh.gulimall.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.ExecutionException;

/**
 * @author: sjh
 * @date: 2021/7/10 下午4:58
 * @description:
 */
@Controller
public class OrderWebController {

    @Autowired
    OrderService orderService;

    @GetMapping("/toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException {
        OrderConfirmVo orderConfirmVo = orderService.confirmOrder();
        model.addAttribute("orderConfirmData", orderConfirmVo);
        return "confirm";
    }

    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo orderSubmitVo, Model model, RedirectAttributes attributes){
        SubmitOrderResponseVo responseVo = orderService.submitOrder(orderSubmitVo);
        if(responseVo.getCode() == 0){
            //成功， 去支付
            model.addAttribute("submitOrderResp", responseVo);
            return "pay";
        }
        else{
            //失败 重新订单确认
            String msg = "下单失败;";
            switch (responseVo.getCode()) {
                case 1:
                    msg += "防重令牌校验失败";
                    break;
                case 2:
                    msg += "商品价格发生变化";
                    break;
                case 3:
                    msg += "锁定库存失败";
            }
            attributes.addFlashAttribute("msg", msg);
            return "redirect:http://order.gulimall.com/toTrade";
        }
    }
}
