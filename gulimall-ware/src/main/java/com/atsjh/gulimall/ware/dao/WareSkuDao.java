package com.atsjh.gulimall.ware.dao;

import com.atsjh.gulimall.ware.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品库存
 * 
 * @author jiahuansong
 * @email jiahuansong@qq.com
 * @date 2021-06-08 15:08:25
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {

    void updateStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("skuNum") Integer skuNum);

    Long hasStock(@Param("id") Long id);

    List<Long> listWareIdsHasStock(@Param("skuId") Long skuId, @Param("count") Integer count);

    Long lockWareSku(@Param("skuId") Long skuId, @Param("num") Integer num, @Param("wareId") Long wareId);

    void unlockStock(@Param("skuId") Long skuId, @Param("skuNum") Integer skuNum, @Param("wareId") Long wareId);
}
