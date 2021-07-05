package com.atsjh.gulimall.product.dao;

import com.atsjh.gulimall.product.entity.AttrEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品属性
 * 
 * @author jiahuansong
 * @email jiahuansong@qq.com
 * @date 2021-06-07 21:01:25
 */
@Mapper
public interface AttrDao extends BaseMapper<AttrEntity> {

    List<Long> getSreachAttrIds(@Param("attrIds") List<Long> attrIds);
}
