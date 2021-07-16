package com.atsjh.gulimall.seckill.service;

import com.atsjh.gulimall.seckill.to.SeckillSkuRedisTo;

import java.util.List;

/**
 * @author: sjh
 * @date: 2021/7/16 下午1:44
 * @description:
 */
public interface SecKillService {
    void uploadSeckillSkuLatest3Days();

    List<SeckillSkuRedisTo> getCurrentSeckillSkus();

    SeckillSkuRedisTo getSeckillSkuInfo(Long skuId);

    String kill(String killId, String key, Integer num) throws InterruptedException;
}
