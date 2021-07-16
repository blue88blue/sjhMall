package com.atsjh.gulimall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atsjh.common.utils.R;
import com.atsjh.gulimall.seckill.feign.CouponFeignService;
import com.atsjh.gulimall.seckill.feign.ProductFeignService;
import com.atsjh.gulimall.seckill.service.SecKillService;
import com.atsjh.gulimall.seckill.to.SeckillSkuRedisTo;
import com.atsjh.gulimall.seckill.vo.SeckillSessionWithSkusVo;
import com.atsjh.gulimall.seckill.vo.SeckillSkuVo;
import com.atsjh.gulimall.seckill.vo.SkuInfoVo;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author: sjh
 * @date: 2021/7/16 下午1:43
 * @description:
 */
@Service
public class SecKillServiceImpl implements SecKillService {
    @Autowired
    CouponFeignService couponFeignService;
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    RedissonClient redissonClient;

    //K: SESSION_CACHE_PREFIX + startTime + "_" + endTime
    //V: sessionId+"-"+skuId的List
    private final String SESSION_CACHE_PREFIX = "seckill:sessions:";

    //K: 固定值SECKILL_CHARE_PREFIX
    //V: hash，k为sessionId+"-"+skuId，v为对应的商品信息SeckillSkuRedisTo
    private final String SECKILL_CHARE_PREFIX = "seckill:skus";

    //K: SKU_STOCK_SEMAPHORE+商品随机码
    //V: 秒杀的库存件数
    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:";    //+商品随机码

    /**
     * 上架最近三天需要
     */
    @Override
    public void uploadSeckillSkuLatest3Days() {
        //查询近三天需要上架的秒杀商品
        R r = couponFeignService.getLate3DaySession();
        if(r.getCode() == 0){
            List<SeckillSessionWithSkusVo> data = r.getData(new TypeReference<List<SeckillSessionWithSkusVo>>() {
            });
            if(data != null && data.size()>1){
                //redis中缓存活动信息
                saveSessionInfos(data);
                //redis中缓存活动相关商品信息
                saveSessionSkuInfos(data);
            }
        }
    }

    //redis中缓存活动信息
    private void saveSessionInfos(List<SeckillSessionWithSkusVo> sessions){
        sessions.stream().forEach(session->{
            String key = SESSION_CACHE_PREFIX + session.getStartTime().getTime() + "_" + session.getEndTime().getTime();
            //当前活动信息未保存过
            if (!redisTemplate.hasKey(key)){
                List<String> values = session.getRelations().stream()
                        .map(sku -> sku.getPromotionSessionId() +"-"+ sku.getSkuId())
                        .collect(Collectors.toList());
                redisTemplate.opsForList().leftPushAll(key,values);
            }
        });
    }
    //redis中缓存活动相关商品信息
    private void saveSessionSkuInfos(List<SeckillSessionWithSkusVo> sessions){
        BoundHashOperations ops = redisTemplate.boundHashOps(SECKILL_CHARE_PREFIX);
        sessions.stream().forEach(session->{
            //保存每个活动中的商品信息
            session.getRelations().stream().forEach(sku->{
                String key = sku.getPromotionSessionId() +"-"+ sku.getSkuId();
                String token = UUID.randomUUID().toString(); //商品令牌
                if(!ops.hasKey(key)){
                    SeckillSkuRedisTo redisTo = new SeckillSkuRedisTo();
                    //sku秒杀的信息
                    BeanUtils.copyProperties(sku, redisTo);
                    //sku的基本信息
                    R r = productFeignService.getSkuInfo(sku.getSkuId());
                    if(r.getCode() == 0){
                        SkuInfoVo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                        });
                        redisTo.setSkuInfoVo(skuInfo);
                    }
                    //设置随机码， 防止在秒杀开始前有恶意请求
                    redisTo.setRandomCode(token);
                    //设置秒杀开始与结束时间
                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());
                    //保存sku信息至redis
                    String jsonString = JSON.toJSONString(redisTo);
                    ops.put(key, jsonString);
                    //如果其他场次上架过就不用加了
                    //使用redisson设置分布式信号量作为库存
                    if(!redisTemplate.hasKey(SKU_STOCK_SEMAPHORE+token)){
                        RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE+token);
                        semaphore.trySetPermits(sku.getSeckillCount());
                    }
                }

            });
        });
    }

    /**
     * 获取当前时间的秒杀活动
     * @return
     */
    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
        //获取上架的所有秒杀活动
        Set<String> keys = redisTemplate.keys(SESSION_CACHE_PREFIX + "*");
        long currentTime = System.currentTimeMillis();
        for(String key : keys){
            //提取当前活动的起止时间
            String replace = key.replace(SESSION_CACHE_PREFIX, "");
            String[] s = replace.split("_");
            long startTime = Long.parseLong(s[0]);
            long endTime = Long.parseLong(s[1]);
            //检查当前活动是否在秒杀时间内
            if(currentTime > startTime && currentTime < endTime){
                //获取本场次的秒杀商品
                List<String> range = redisTemplate.opsForList().range(key, -100, 100);
                BoundHashOperations<String, String, String> skuOps = redisTemplate.boundHashOps(SECKILL_CHARE_PREFIX);
                List<SeckillSkuRedisTo> collect = range.stream().map(skuKey -> {
                    String s1 = skuOps.get(skuKey);
                    SeckillSkuRedisTo seckillSkuVo = JSON.parseObject(s1, SeckillSkuRedisTo.class);
                    return seckillSkuVo;
                }).collect(Collectors.toList());
                return collect;
            }
        }
        return null;
    }

    @Override
    public SeckillSkuRedisTo getSeckillSkuInfo(Long skuId) {
        return null;
    }

    @Override
    public String kill(String killId, String key, Integer num) throws InterruptedException {
        return null;
    }
}
