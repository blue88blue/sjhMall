package com.atsjh.gulimall.ware.exception;

/**
 * @author: sjh
 * @date: 2021/7/12 下午5:11
 * @description:
 */
public class NoStockException extends RuntimeException{
    private Long skuId;
    public NoStockException(Long skuId){
        super("商品"+skuId+"的库存锁定失败");
    }
}
