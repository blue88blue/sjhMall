package com.atsjh.gulimall.member.feign;

import com.atsjh.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author: sjh
 * @date: 2021/6/8 下午6:37
 * @description:
 * */
//告诉spring cloud这个接口是一个远程客户端，要调用coupon服务(nacos中找到)，具体是调用coupon服务的/coupon/coupon/member/list对应的方法
@FeignClient("gulimall-coupon")
public interface CouponFeignService {
    // 远程服务的url
    @RequestMapping("coupon/coupon/member/list")
    public R memberCoupon();
}
