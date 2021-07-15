package com.atsjh.gulimall.order.feign;

import com.atsjh.common.utils.R;
import com.atsjh.gulimall.order.vo.WareSkuLockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author: sjh
 * @date: 2021/7/12 下午4:04
 * @description:
 */
@FeignClient("gulimall-ware")
public interface WareFeignService {

    @PostMapping("/ware/waresku/lock/order")
    R lockOder(@RequestBody WareSkuLockVo lockVo);
}
