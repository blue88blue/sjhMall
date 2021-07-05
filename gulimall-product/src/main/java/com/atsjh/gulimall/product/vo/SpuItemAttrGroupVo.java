package com.atsjh.gulimall.product.vo;

import lombok.Data;

import java.util.List;

/**
 * @author: sjh
 * @date: 2021/7/4 上午10:54
 * @description:
 */
@Data
public class SpuItemAttrGroupVo {

    private String groupName;

    private List<Attr> attrs;
}
