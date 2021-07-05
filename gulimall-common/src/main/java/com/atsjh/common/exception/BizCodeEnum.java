package com.atsjh.common.exception;

/**
 * @author: sjh
 * @date: 2021/6/13 下午8:59
 * @description:
 */
public enum BizCodeEnum {
    UNKNOW_EXEPTION(10000,"系统未知异常"),

    VALID_EXCEPTION( 10001,"参数格式校验失败"),

    PRODUCT_UP_EXCEPTION(11000, "商品上架错误");

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
