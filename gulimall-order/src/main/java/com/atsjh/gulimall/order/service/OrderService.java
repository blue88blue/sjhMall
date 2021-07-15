package com.atsjh.gulimall.order.service;

import com.atsjh.common.to.mq.OrderTo;
import com.atsjh.gulimall.order.vo.OrderConfirmVo;
import com.atsjh.gulimall.order.vo.OrderSubmitVo;
import com.atsjh.gulimall.order.vo.SubmitOrderResponseVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atsjh.common.utils.PageUtils;
import com.atsjh.gulimall.order.entity.OrderEntity;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 订单
 *
 * @author jiahuansong
 * @email jiahuansong@qq.com
 * @date 2021-06-08 15:02:09
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException;

    SubmitOrderResponseVo submitOrder(OrderSubmitVo orderSubmitVo);

    OrderEntity getOrderByOrderSn(String orderSn);

    void closeOrder(OrderEntity order);
}

