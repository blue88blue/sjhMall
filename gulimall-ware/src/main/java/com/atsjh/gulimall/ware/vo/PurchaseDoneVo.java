package com.atsjh.gulimall.ware.vo;

import lombok.Data;

import java.util.List;

/**
 * @author: sjh
 * @date: 2021/6/23 下午8:59
 * @description:
 */
@Data
public class PurchaseDoneVo {
    /**
     *    id: 123,//采购单id
     *    items: [{itemId:1,status:4,reason:""}]//完成/失败的需求详情
     */
    private Long id;
    private List<PurchaseItemDoneVo> items;
}
