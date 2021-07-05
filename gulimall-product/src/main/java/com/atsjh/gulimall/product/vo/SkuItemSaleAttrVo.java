package com.atsjh.gulimall.product.vo;

import lombok.Data;

import java.util.List;

/**
 * @author: sjh
 * @date: 2021/7/4 上午10:51
 * @description:
 */
@Data
public class SkuItemSaleAttrVo {
    private Long attrId;
    private String attrName;

    private List<AttrValueWithSkuIdVO> attrValues;
}
