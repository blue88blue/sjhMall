package com.atsjh.gulimall.product.dao;

import com.atsjh.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author jiahuansong
 * @email jiahuansong@qq.com
 * @date 2021-06-08 14:18:44
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
