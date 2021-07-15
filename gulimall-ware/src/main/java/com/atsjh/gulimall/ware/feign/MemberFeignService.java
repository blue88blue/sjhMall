package com.atsjh.gulimall.ware.feign;

import com.atsjh.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author: sjh
 * @date: 2021/7/12 下午7:15
 * @description:
 */
@FeignClient("gulimall-member")
public interface MemberFeignService {
    @RequestMapping("member/memberreceiveaddress/info/{id}")
    R info(@PathVariable("id") Long id);
}