package com.atsjh.gulimall.ware.vo;

import lombok.Data;

import java.util.List;

/**
 * @author: sjh
 * @date: 2021/6/23 下午7:38
 * @description:
 */
@Data
public class MergeVo {
    /**
     *   purchaseId: 1, //整单id
     *   items:[1,2,3,4] //合并项集合
      */
    private Long purchaseId;
    private List<Long> items;
}
