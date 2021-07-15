package com.atsjh.gulimall.ware.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.HashMap;

/**
 * @author: sjh
 * @date: 2021/7/10 上午11:00
 * @description:
 */
@Configuration
public class RabbitMQConfig {
    @Autowired
    RabbitTemplate rabbitTemplate;

//
//    @RabbitListener(queues={"order.release.order.queue"})
//    public void testListener(Message message, OrderEntity orderEntity, Channel channel){
//        System.out.println("订单过期"+orderEntity.getOrderSn());
//        try {
//            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    /**
     * 延迟队列
     * @return
     */
    @Bean
    public Queue orderDelayQueue(){
        HashMap<String, Object> arguments = new HashMap<>();
        //死信交换机
        arguments.put("x-dead-letter-exchange", "ware-event-exchange");
        //死信路由键
        arguments.put("x-dead-letter-routing-key", "ware.release.ware");
        // 消息过期时间 1分钟
        arguments.put("x-message-ttl", 60000);

        //Queue(String name, boolean durable, boolean exclusive, boolean autoDelete, @Nullable Map<String, Object> arguments)
        return new Queue("ware.delay.queue", true, false, false, arguments);
    }
    /**
     * 普通队列， 将死信放入其中
     *
     * @return
     */
    @Bean
    public Queue orderReleaseQueue() {
        return new Queue("ware.release.ware.queue", true, false, false);
    }

    /**
     * 交换机
     * @return
     */
    @Bean
    public Exchange orderEventExchange() {
        return new TopicExchange("ware-event-exchange", true, false);
    }

    /**
     * 创建订单的binding
     * @return
     */
    @Bean
    public Binding orderCreateBinding() {
        return new Binding("ware.delay.queue",
                Binding.DestinationType.QUEUE,
                "ware-event-exchange",
                "ware.create.ware",
                null);
    }

    /**
     * 过期消息的交换路由与队列绑定
     * @return
     */
    @Bean
    public Binding orderReleaseBinding() {
        return new Binding("ware.release.ware.queue",
                Binding.DestinationType.QUEUE,
                "ware-event-exchange",
                "ware.release.ware",
                null);
    }


    /**
     * Json转换器
     * @return
     */
    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 生产者端的可靠投递回调函数
     */
    @PostConstruct
    public void initRabbitTemplate(){
        //发送到rabbit服务器后回调
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            @Override
            public void confirm(CorrelationData correlationData, boolean b, String s) {
                System.out.println(correlationData+"||"+b+"||"+s);
            }
        });
    }

}
