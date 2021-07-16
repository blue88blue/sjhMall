package com.atsjh.gulimall.coupon.service.impl;

import com.atsjh.gulimall.coupon.entity.SeckillSkuRelationEntity;
import com.atsjh.gulimall.coupon.service.SeckillSkuRelationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atsjh.common.utils.PageUtils;
import com.atsjh.common.utils.Query;

import com.atsjh.gulimall.coupon.dao.SeckillSessionDao;
import com.atsjh.gulimall.coupon.entity.SeckillSessionEntity;
import com.atsjh.gulimall.coupon.service.SeckillSessionService;

import javax.swing.text.DateFormatter;

@Slf4j
@Service("seckillSessionService")
public class SeckillSessionServiceImpl extends ServiceImpl<SeckillSessionDao, SeckillSessionEntity> implements SeckillSessionService {

    @Autowired
    SeckillSkuRelationService relationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SeckillSessionEntity> page = this.page(
                new Query<SeckillSessionEntity>().getPage(params),
                new QueryWrapper<SeckillSessionEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 获取三天内的秒杀活动
     * @return
     */
    @Override
    public List<SeckillSessionEntity> getLate3DaySession() {
        List<SeckillSessionEntity> list = this.list(new QueryWrapper<SeckillSessionEntity>().between("start_time", startTime(), endTime()));
        log.info("近3天秒杀活动：{}",list);
        if (list != null && list.size() > 0){
            List<SeckillSessionEntity> collect = list.stream().map(session -> {
                Long id = session.getId();
                List<SeckillSkuRelationEntity> skuRelationEntities = relationService.list(new QueryWrapper<SeckillSkuRelationEntity>().eq("promotion_id", id));
                log.info("秒杀商品：{}",skuRelationEntities);
                session.setRelations(skuRelationEntities);
                return session;
            }).collect(Collectors.toList());
            return collect;
        }
        return null;
    }
    private LocalDateTime startTime(){
        LocalDate now = LocalDate.now();
        LocalTime min = LocalTime.MIN;
        LocalDateTime start = LocalDateTime.of(now, min);
        start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return start;
    }
    private LocalDateTime endTime(){
        LocalDate now = LocalDate.now();
        LocalDate localDate = now.plusDays(2);
        LocalTime min = LocalTime.MAX;
        LocalDateTime end = LocalDateTime.of(localDate, min);
        end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return end;
    }
}