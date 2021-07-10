package com.atsjh.gulimall.order.service.impl;

import com.atsjh.gulimall.order.entity.OrderEntity;
import com.atsjh.gulimall.order.entity.OrderReturnReasonEntity;
import com.rabbitmq.client.Channel;
import org.aspectj.weaver.ast.Or;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atsjh.common.utils.PageUtils;
import com.atsjh.common.utils.Query;

import com.atsjh.gulimall.order.dao.OrderItemDao;
import com.atsjh.gulimall.order.entity.OrderItemEntity;
import com.atsjh.gulimall.order.service.OrderItemService;

@RabbitListener(queues = {"sjhmall.news"})
@Service("orderItemService")
public class OrderItemServiceImpl extends ServiceImpl<OrderItemDao, OrderItemEntity> implements OrderItemService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderItemEntity> page = this.page(
                new Query<OrderItemEntity>().getPage(params),
                new QueryWrapper<OrderItemEntity>()
        );

        return new PageUtils(page);
    }

    @RabbitHandler
    public void getMsg(Message message, OrderEntity orderEntity, Channel channel) throws IOException {
        System.out.println("收到订单"+orderEntity);
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        channel.basicAck(deliveryTag, false);
    }

    @RabbitHandler
    public void getMsg(Message message, OrderReturnReasonEntity orderReturnReasonEntity, Channel channel) throws IOException {
        System.out.println("收到订单退回"+orderReturnReasonEntity);

        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        channel.basicAck(deliveryTag, false);
    }

}