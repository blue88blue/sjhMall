package com.atsjh.gulimall.product.service;

import com.atsjh.gulimall.product.vo.AttrRespVo;
import com.atsjh.gulimall.product.vo.AttrVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atsjh.common.utils.PageUtils;
import com.atsjh.gulimall.product.entity.AttrEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品属性
 *
 * @author jiahuansong
 * @email jiahuansong@qq.com
 * @date 2021-06-07 21:01:25
 */
public interface AttrService extends IService<AttrEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveDetail(AttrVo attr);

    PageUtils queryBaseList(Map<String, Object> params, Long categoryId, String type);

    AttrRespVo getAttrInfo(Long attrId);


    void updateDetail(AttrRespVo attr);

    List<AttrEntity> getRelatedAttr(Long attrGroupId);

    PageUtils geNoAttrRelation(Map<String, Object> params, Long attrGroupId);


    List<Long> getSreachAttrIds(List<Long> attrIds);
}

