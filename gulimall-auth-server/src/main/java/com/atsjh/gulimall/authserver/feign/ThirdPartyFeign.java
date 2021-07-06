package com.atsjh.gulimall.authserver.feign;

import com.atsjh.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author: sjh
 * @date: 2021/7/5 下午3:52
 * @description:
 */
@FeignClient("gulimall-third-party")
public interface ThirdPartyFeign {

    @GetMapping("/sms/sendCode")
    public R sendCode(@RequestParam("phone") String phone, @RequestParam("code") String code);
}
