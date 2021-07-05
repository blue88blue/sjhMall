package com.atsjh.gulimall.ware.vo;

import lombok.Data;

import java.time.Period;
import java.util.List;

/**
 * @author: sjh
 * @date: 2021/6/23 下午9:00
 * @description:
 */
@Data
public class PurchaseItemDoneVo {
    /**
     * [{itemId:1,status:4,reason:""}]//完成/失败的需求详情
     */
    private Long itemId;
    private Integer status;
    private String reason;
}
