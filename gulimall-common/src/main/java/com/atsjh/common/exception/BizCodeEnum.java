package com.atsjh.common.exception;

/**
 * @author: sjh
 * @date: 2021/6/13 下午8:59
 * @description:
 */
public enum BizCodeEnum {
    UNKNOW_EXEPTION(10000,"系统未知异常"),

    VALID_EXCEPTION( 10001,"参数格式校验失败"),

    PRODUCT_UP_EXCEPTION(11000, "商品上架错误"),

    SMS_CODE_EXCEPTION(10200, "验证码请求过于频繁"),

    USERNAME_EXIST_EXCEPTION(15001, "用户名已存在"),

    PHONE_EXIST_EXCEPTION(15002, "手机号已注册"),

    PASSWORD_INVALID_EXCEPTION(15003, "用户名或密码错误")
    ;

    private int code;
    private String msg;

    BizCodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

}
