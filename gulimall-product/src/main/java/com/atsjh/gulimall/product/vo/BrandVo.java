package com.atsjh.gulimall.product.vo;

import lombok.Data;
import org.apache.commons.lang.StringUtils;

/**
 * @author: sjh
 * @date: 2021/6/21 下午9:48
 * @description:
 */

@Data
public class BrandVo {
    /**
     "brandId": 0,
     "brandName": "string",
     */
    private Long brandId;
    private String brandName;
}
