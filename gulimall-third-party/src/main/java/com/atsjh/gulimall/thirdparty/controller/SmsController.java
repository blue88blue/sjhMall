package com.atsjh.gulimall.thirdparty.controller;

import com.atsjh.common.utils.R;
import com.atsjh.gulimall.thirdparty.component.SmsConponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: sjh
 * @date: 2021/7/5 下午3:39
 * @description:
 */
@RestController
@RequestMapping("/sms")
public class SmsController {

    @Autowired
    SmsConponent smsConponent;

    /**
     * 给别的服务提供验证码服务
     * @param phone
     * @param code
     * @return
     */
    @GetMapping("/sendCode")
    public R sendCode(@RequestParam("phone") String phone, @RequestParam("code") String code){
        smsConponent.sendSmsCode(phone, code);
        return R.ok();
    }
}
