package com.atsjh.gulimall.authserver.vo;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

/**
 * @author: sjh
 * @date: 2021/7/5 下午4:53
 * @description:
 */
@Data
public class UserRegistVo {


    @NotEmpty(message = "用户名必须提交")
    @Length(min = 3, max = 20, message = "用户名长度要在3-20")
    private String userName;

    @NotEmpty(message = "密码必须填写")
    private String password;

    @NotEmpty(message = "手机号必须填写")
    @Pattern(regexp = "^[1][3-9][0-9]{9}$", message = "手机号不可用")
    private String phone;

    @NotEmpty(message = "请填写验证码")
    private String code;

}
