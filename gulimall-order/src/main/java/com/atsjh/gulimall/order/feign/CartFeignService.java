package com.atsjh.gulimall.order.feign;

import com.atsjh.gulimall.order.vo.OrderItemVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * @author: sjh
 * @date: 2021/7/11 下午2:05
 * @description:
 */
@FeignClient("gulimall-cart")
public interface CartFeignService {

    @GetMapping("/getItemsForOrder")
    List<OrderItemVo> getItemsForOrder();

    @GetMapping("/getCheckedItems")
    List<OrderItemVo> getCheckedItems();
}
