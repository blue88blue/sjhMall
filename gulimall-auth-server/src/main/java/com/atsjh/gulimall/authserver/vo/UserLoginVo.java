package com.atsjh.gulimall.authserver.vo;

import lombok.Data;
import lombok.ToString;

/**
 * @author: sjh
 * @date: 2021/7/6 下午1:33
 * @description:
 */
@ToString
@Data
public class UserLoginVo {
    private String loginacct;
    private String password;
}
