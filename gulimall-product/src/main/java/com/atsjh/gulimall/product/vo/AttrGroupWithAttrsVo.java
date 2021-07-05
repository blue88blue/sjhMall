package com.atsjh.gulimall.product.vo;

import com.atsjh.gulimall.product.entity.AttrEntity;
import com.atsjh.gulimall.product.entity.AttrGroupEntity;
import lombok.Data;

import java.util.List;

/**
 * @author: sjh
 * @date: 2021/6/22 上午11:38
 * @description:
 */
@Data
public class AttrGroupWithAttrsVo extends AttrGroupEntity {
    List<AttrVo> attrs;
}
