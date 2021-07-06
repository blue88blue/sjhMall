package com.atsjh.gulimall.member.vo;

import lombok.Data;

/**
 * @author: sjh
 * @date: 2021/7/6 下午7:01
 * @description:
 */
@Data
public class SocialUser {
    private String access_token;
    private String token_type;
    private Long expires_in;
    private String refresh_token;
    private String scope;
    private String created_at;
}
