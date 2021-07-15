package com.atsjh.gulimall.ware.service;

import com.atsjh.common.to.mq.OrderTo;
import com.atsjh.common.to.mq.StockLockedTo;
import com.atsjh.gulimall.ware.vo.SkuHasStochVo;
import com.atsjh.gulimall.ware.vo.WareSkuLockVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atsjh.common.utils.PageUtils;
import com.atsjh.gulimall.ware.entity.WareSkuEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author jiahuansong
 * @email jiahuansong@qq.com
 * @date 2021-06-08 15:08:25
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStochVo> hasStock(List<Long> skuIds);

    boolean orderLockStock(WareSkuLockVo lockVo);

    void unlock(StockLockedTo stockLockedTo);

    void unlock(OrderTo orderTo);
}

