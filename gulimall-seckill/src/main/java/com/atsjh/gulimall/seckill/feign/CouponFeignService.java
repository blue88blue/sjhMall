package com.atsjh.gulimall.seckill.feign;

import com.atsjh.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author: sjh
 * @date: 2021/7/16 下午2:33
 * @description:
 */
@FeignClient("gulimall-coupon")
public interface CouponFeignService {

    @GetMapping("/coupon/seckillsession/late3DaySession")
    R getLate3DaySession();
}
