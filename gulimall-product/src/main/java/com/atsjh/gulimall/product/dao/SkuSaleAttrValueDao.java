package com.atsjh.gulimall.product.dao;

import com.atsjh.gulimall.product.entity.SkuSaleAttrValueEntity;
import com.atsjh.gulimall.product.vo.SkuItemSaleAttrVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * sku销售属性&值
 * 
 * @author jiahuansong
 * @email jiahuansong@qq.com
 * @date 2021-06-07 21:01:25
 */
@Mapper
public interface SkuSaleAttrValueDao extends BaseMapper<SkuSaleAttrValueEntity> {

    List<SkuItemSaleAttrVo> getSaleAttrBySpuId(@Param("spuId") Long spuId);

    List<String> getSaleAttrBySkuId(@Param("skuId") Long skuId);
}
