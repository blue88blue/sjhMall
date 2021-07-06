package com.atsjh.gulimall.authserver.controller;

import com.alibaba.fastjson.TypeReference;
import com.atsjh.common.constant.AuthServerConstant;
import com.atsjh.common.exception.BizCodeEnum;
import com.atsjh.common.utils.R;
import com.atsjh.gulimall.authserver.feign.MemberFeignService;
import com.atsjh.gulimall.authserver.feign.ThirdPartyFeign;
import com.atsjh.gulimall.authserver.vo.UserLoginVo;
import com.atsjh.gulimall.authserver.vo.UserRegistVo;
import com.mysql.cj.util.DnsSrv;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.xml.transform.Result;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author: sjh
 * @date: 2021/7/5 下午1:50
 * @description:
 */
@Controller
public class LoginController {
    @Autowired
    ThirdPartyFeign thirdPartyFeign;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    MemberFeignService memberFeignService;

    @GetMapping("/login.html")
    public String gotoLogin(){
        return "login";
    }

    @GetMapping("/reg.html")
    public String gotoReg(){
        return "reg";
    }

    @GetMapping("/sms/sendCode")
    public R sendCode(@RequestParam("phone") String phone){
        //60s 接口防刷
        String s = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CAHCE_PREFIX + phone);
        if(!StringUtils.isEmpty(s)){
            String[] code = s.split("_");
            if(System.currentTimeMillis() - Long.parseLong(code[1]) < 60000){
                return R.error(BizCodeEnum.SMS_CODE_EXCEPTION.getCode(), BizCodeEnum.SMS_CODE_EXCEPTION.getMsg());
            }
        }

        String code = UUID.randomUUID().toString().substring(0, 5);
        //放缓存中方便校验
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CAHCE_PREFIX+phone,
                code + "_" + System.currentTimeMillis(), 10, TimeUnit.MINUTES);

        thirdPartyFeign.sendCode(phone, code);
        return R.ok();
    }

    /**
     * TODO 分布式下session问题
     * 用户注册
     * @param vo
     * @param result
     * @param
     * @return
     */
    @PostMapping("/register")
    public String regist(@Valid UserRegistVo vo, BindingResult result, RedirectAttributes redirectAttributes){
        if(result.hasErrors()){
            //将错误消息放入model中， 转发给reg页面回显错误信息
            Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/reg.html";
        }
        //检查验证码
        String s = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CAHCE_PREFIX + vo.getPhone());
        if(!StringUtils.isEmpty(s)){
            String code = s.split("_")[0];
            if(code.equalsIgnoreCase(vo.getCode())){
                //删除验证码
                redisTemplate.delete(AuthServerConstant.SMS_CODE_CAHCE_PREFIX + vo.getPhone());
                // TODO 交给远程member服务注册
                R r = memberFeignService.memberRegist(vo);
                if(r.getCode() == 0){
                    return "redirect:http://auth.gulimall.com/login.html";
                }
                else{
                    HashMap<String, String> map = new HashMap<>();
                    String msg = (String) r.get("msg");
                    map.put("msg", msg);
                    redirectAttributes.addFlashAttribute("errors", map);
                    return "redirect:http://auth.gulimall.com/reg.html";
                }
            }
            else{
                HashMap<String, String> errors = new HashMap<>();
                errors.put("code", "验证码错误");
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:http://auth.gulimall.com/reg.html";
            }
        }
        else{
            HashMap<String, String> errors = new HashMap<>();
            errors.put("code", "验证码错误");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/reg.html";
        }
    }


    @PostMapping("/login")
    public String login(UserLoginVo vo, RedirectAttributes redirectAttributes){
        System.out.println(vo);
        R r = memberFeignService.login(vo);
        if(r.getCode() == 0){
            return "redirect:http://gulimall.com";
        }
        else{
            HashMap<String, String> errors = new HashMap<>();
            errors.put("msg", (String) r.get("msg"));
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }

    }
}
