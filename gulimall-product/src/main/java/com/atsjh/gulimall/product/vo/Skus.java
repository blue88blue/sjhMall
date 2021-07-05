/**
  * Copyright 2021 json.cn 
  */
package com.atsjh.gulimall.product.vo;
import lombok.Data;
import org.apache.tomcat.jni.BIOCallback;

import java.math.BigDecimal;
import java.util.List;

/**
 * Auto-generated: 2021-06-22 16:10:19
 *
 * @author json.cn (i@json.cn)
 * @website http://www.json.cn/java2pojo/
 */
@Data
public class Skus {

    private List<Attr> attr;
    private String skuName;
    private BigDecimal price;
    private String skuTitle;
    private String skuSubtitle;
    private List<Images> images;
    private List<String> descar;
    private Integer fullCount;
    private BigDecimal discount;
    private Integer countStatus;
    private BigDecimal fullPrice;
    private BigDecimal reducePrice;
    private Integer priceStatus;
    private List<MemberPrice> memberPrice;


}