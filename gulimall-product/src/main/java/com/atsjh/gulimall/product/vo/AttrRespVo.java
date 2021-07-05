package com.atsjh.gulimall.product.vo;

import com.atsjh.gulimall.product.entity.AttrEntity;
import lombok.Data;
import org.apache.commons.lang.StringUtils;

/**
 * @author: sjh
 * @date: 2021/6/20 下午9:15
 * @description:
 */
@Data
public class AttrRespVo extends AttrEntity {

    /**
     * 			"catelogName": "手机/数码/手机", //所属分类名字
     * 			"groupName": "主体", //所属分组名字
     *
     * 		    "catelogPath": [2, 34, 225] //分类完整路径
     * 		    "attrGroupId": 1, //分组id
     * 		    "catelogId": 225, //分类id
     */
    private String catelogName;

    private String groupName;

    private Long[] catelogPath;

    private Long attrGroupId;
}
