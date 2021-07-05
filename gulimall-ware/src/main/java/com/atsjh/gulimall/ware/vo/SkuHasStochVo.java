package com.atsjh.gulimall.ware.vo;

import com.sun.org.apache.xpath.internal.operations.Bool;
import lombok.Data;

/**
 * @author: sjh
 * @date: 2021/6/26 下午7:10
 * @description:
 */
@Data
public class SkuHasStochVo {
    private Long skuId;
    private Boolean hasStock;
}
