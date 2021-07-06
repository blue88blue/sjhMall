package com.atsjh.gulimall.member.exception;

/**
 * @author: sjh
 * @date: 2021/7/5 下午7:30
 * @description:
 */
public class UserNameUniqueException extends RuntimeException {

    public UserNameUniqueException(){
        super("用户名已存在");
    }
}
