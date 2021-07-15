package com.atsjh.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atsjh.common.utils.PageUtils;
import com.atsjh.gulimall.product.entity.SpuInfoEntity;

import java.util.Map;

/**
 * spu信息
 *
 * @author jiahuansong
 * @email jiahuansong@qq.com
 * @date 2021-06-07 21:01:25
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPageByCondition(Map<String, Object> params);

    void up(Long spuId);

    SpuInfoEntity getSpuBySkuId(Long skuId);
}

