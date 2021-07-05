package com.atsjh.gulimall.authserver.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author: sjh
 * @date: 2021/7/5 下午1:50
 * @description:
 */
@Controller
public class LoginController {

    @GetMapping("/login.html")
    public String gotoLogin(){
        return "login";
    }

    @GetMapping("/reg.html")
    public String gotoReg(){
        return "reg";
    }
}
