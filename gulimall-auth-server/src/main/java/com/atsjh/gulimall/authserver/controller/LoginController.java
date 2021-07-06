package com.atsjh.gulimall.authserver.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atsjh.common.constant.AuthServerConstant;
import com.atsjh.common.exception.BizCodeEnum;
import com.atsjh.common.utils.R;
import com.atsjh.gulimall.authserver.feign.MemberFeignService;
import com.atsjh.gulimall.authserver.feign.ThirdPartyFeign;
import com.atsjh.gulimall.authserver.utils.HttpUtils;
import com.atsjh.gulimall.authserver.vo.MemberResponseVo;
import com.atsjh.gulimall.authserver.vo.SocialUser;
import com.atsjh.gulimall.authserver.vo.UserLoginVo;
import com.atsjh.gulimall.authserver.vo.UserRegistVo;
import com.mysql.cj.util.DnsSrv;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
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
import java.io.InputStream;
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
@Slf4j
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


    @GetMapping("/oauth2/gitee/success")
    public String authLogin(@RequestParam("code") String code) throws Exception {
        //将code发给社交服务器换取token
        HashMap<String, String> querys = new HashMap<>();
        querys.put("grant_type", "authorization_code");
        querys.put("code", code);
        querys.put("client_id", "0733fbbd75cdcbb07ee6cb6b6f2722d3ab9503a1762aed94d9022b256de8e537");
        querys.put("redirect_uri", "http://auth.gulimall.com/oauth2/gitee/success");
        querys.put("client_secret", "80ed1d1a815e6841bc638ddfb96a6583c04b4db9930d5dd37ddb3d71f5a275ce");
        //发送给gitee服务器获取token
        HttpResponse reponse = HttpUtils.doPost("https://gitee.com", "/oauth/token", "post", new HashMap<>(), querys, new HashMap<>());

        if(reponse.getStatusLine().getStatusCode() == 200){
            //token等信息vo
            String s = EntityUtils.toString(reponse.getEntity());
            SocialUser socialUser = JSON.parseObject(s, SocialUser.class);
            //判断用户是否是第一次登录， 如果是则需要注册， 在member表中增加token等字段
            R r = memberFeignService.socialLogin(socialUser);
            if(r.getCode() == 0){
                String jsonString = JSON.toJSONString(r.get("data"));
                MemberResponseVo memberResponseVo = JSON.parseObject(jsonString, new TypeReference<MemberResponseVo>(){});
                log.info("登录成功， 用户{}", memberResponseVo);
                return "redirect:http://gulimall.com";
            }
            else{
                return "redirect:http://auth.gulimall.com/login.html";
            }
        }
        else {
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }
}
