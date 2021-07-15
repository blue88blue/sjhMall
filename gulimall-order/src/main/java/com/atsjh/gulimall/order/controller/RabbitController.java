package com.atsjh.gulimall.order.controller;

import com.atsjh.gulimall.order.entity.OrderEntity;
import com.atsjh.gulimall.order.entity.OrderReturnReasonEntity;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author: sjh
 * @date: 2021/7/10 下午2:30
 * @description:
 */
@Controller
public class RabbitController {
    @Autowired
    RabbitTemplate rabbitTemplate;

    @GetMapping("{page}.html")
    public String page(@PathVariable("page") String page){
        return page;
    }


    @GetMapping("sendMQ")
    @ResponseBody
    public String send(){
        for (int i = 0; i < 10; i++) {
            if(i % 2 == 0){
                OrderEntity orderEntity = new OrderEntity();
                orderEntity.setId(3541L);
                orderEntity.setBillHeader("haha"+i);
                rabbitTemplate.convertAndSend("myexchange", "11.news", orderEntity);
            }
            else{
                OrderReturnReasonEntity orderReturnReasonEntity = new OrderReturnReasonEntity();
                orderReturnReasonEntity.setId(134L);
                orderReturnReasonEntity.setName("HHHH"+i);
                rabbitTemplate.convertAndSend("myexchange", "11.news", orderReturnReasonEntity);
            }
        }
        return "ok";
    }
}
