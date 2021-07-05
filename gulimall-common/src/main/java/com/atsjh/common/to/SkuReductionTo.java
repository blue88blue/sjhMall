package com.atsjh.common.to;

import com.sun.org.apache.bcel.internal.generic.BIPUSH;
import com.sun.org.apache.bcel.internal.generic.INEG;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author: sjh
 * @date: 2021/6/22 下午6:20
 * @description:
 */
@Data
public class SkuReductionTo {
    private Long skuId;
    private Integer fullCount;
    private BigDecimal discount;
    private Integer countStatus;
    private BigDecimal fullPrice;
    private BigDecimal reducePrice;
    private Integer priceStatus;
    private List<MemberPrice> memberPrice;
}
