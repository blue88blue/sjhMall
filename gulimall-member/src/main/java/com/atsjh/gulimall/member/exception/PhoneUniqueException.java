package com.atsjh.gulimall.member.exception;

/**
 * @author: sjh
 * @date: 2021/7/5 下午7:29
 * @description:
 */
public class PhoneUniqueException extends RuntimeException {
    public PhoneUniqueException(){
        super("手机号已被注册");
    }
}
