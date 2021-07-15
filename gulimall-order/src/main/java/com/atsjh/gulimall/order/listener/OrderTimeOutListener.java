package com.atsjh.gulimall.order.listener;

import com.atsjh.common.to.mq.OrderTo;
import com.atsjh.gulimall.order.entity.OrderEntity;
import com.atsjh.gulimall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author: sjh
 * @date: 2021/7/15 下午2:04
 * @description:
 */
@Slf4j
@Component
@RabbitListener(queues={"order.release.order.queue"})
public class OrderTimeOutListener {
    @Autowired
    OrderService orderService;

    @RabbitHandler
    public void closeOrder(Message message, OrderEntity order, Channel channel) throws IOException {
        log.info("收到订单超时消息. 如未付款，将关闭订单：{}", order.getOrderSn());
        try {
            orderService.closeOrder(order);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
        }
    }
}
