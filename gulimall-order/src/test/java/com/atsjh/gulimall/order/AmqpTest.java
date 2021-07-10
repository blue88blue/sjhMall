package com.atsjh.gulimall.order;

import com.atsjh.common.constant.WareConstant;
import com.atsjh.gulimall.order.entity.OrderEntity;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author: sjh
 * @date: 2021/7/10 上午10:44
 * @description:
 */
@SpringBootTest
public class AmqpTest {
    @Autowired
    AmqpAdmin amqpAdmin;

    @Autowired
    RabbitTemplate rabbitTemplate;

    /**
     * 创建交换机
     */
    @Test
    public void test(){
        DirectExchange directExchange = new DirectExchange("hello-java-exchange", true, false, null);
        amqpAdmin.declareExchange(directExchange);
    }

    /**
     * 发消息
     */
    @Test
    public void test1(){
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setId(3541L);
        orderEntity.setBillHeader("haha");
        rabbitTemplate.convertAndSend("myexchange", "sjhmall.news", orderEntity);
    }
}
