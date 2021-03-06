package com.atsjh.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atsjh.common.to.mq.OrderTo;
import com.atsjh.common.to.mq.StockDetailTo;
import com.atsjh.common.to.mq.StockLockedTo;
import com.atsjh.common.utils.R;
import com.atsjh.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.atsjh.gulimall.ware.entity.WareOrderTaskEntity;
import com.atsjh.gulimall.ware.enume.OrderStatusEnum;
import com.atsjh.gulimall.ware.enume.WareTaskStatusEnum;
import com.atsjh.gulimall.ware.exception.NoStockException;
import com.atsjh.gulimall.ware.feign.OrderFeignService;
import com.atsjh.gulimall.ware.feign.ProductFeignService;
import com.atsjh.gulimall.ware.service.WareOrderTaskDetailService;
import com.atsjh.gulimall.ware.service.WareOrderTaskService;
import com.atsjh.gulimall.ware.vo.OrderItemVo;
import com.atsjh.gulimall.ware.vo.SkuHasStochVo;
import com.atsjh.gulimall.ware.vo.WareSkuLockVo;
import lombok.Data;
import org.hibernate.validator.constraints.URL;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atsjh.common.utils.PageUtils;
import com.atsjh.common.utils.Query;

import com.atsjh.gulimall.ware.dao.WareSkuDao;
import com.atsjh.gulimall.ware.entity.WareSkuEntity;
import com.atsjh.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    WareOrderTaskService wareOrderTaskService;

    @Autowired
    WareOrderTaskDetailService wareOrderTaskDetailService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    OrderFeignService orderFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("sku_id", skuId).eq("ware_id", wareId);
        Integer num = this.baseMapper.selectCount(wrapper);
        if(num <= 0){
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setStockLocked(0);
            // ????????????skuName??? ?????????????????????????????????
            //TODO ?????????????????????????????????
            try {
                R info = productFeignService.info(skuId);
                if(info.getCode() == 0){
                    Map<String, Object> skuInfo = (Map)info.get("skuInfo");
                    wareSkuEntity.setSkuName((String) skuInfo.get("skuName"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.save(wareSkuEntity);
        }
        else{
            this.baseMapper.updateStock(skuId, wareId, skuNum);
        }
    }

    /**
     * ??????skuId?????????sku?????????
     * @param skuIds
     * @return
     */
    @Override
    public List<SkuHasStochVo> hasStock(List<Long> skuIds) {

        List<SkuHasStochVo> collect = skuIds.stream().map(id -> {
            SkuHasStochVo vo = new SkuHasStochVo();
            vo.setSkuId(id);
            Long stock = this.baseMapper.hasStock(id);
            vo.setHasStock(stock == null ? false : stock > 0);
            return vo;
        }).collect(Collectors.toList());
        return collect;
    }

    /**
     * ????????????
     * @param lockVo
     * @return
     */
    @Transactional
    @Override
    public boolean orderLockStock(WareSkuLockVo lockVo) {
        //?????????????????????
        WareOrderTaskEntity wareOrderTaskEntity = new WareOrderTaskEntity();
        wareOrderTaskEntity.setOrderSn(lockVo.getOrderSn());
        wareOrderTaskEntity.setCreateTime(new Date());
        wareOrderTaskService.save(wareOrderTaskEntity);

        List<OrderItemVo> locks = lockVo.getLocks();
        List<SkuLockVo> lockVos = locks.stream().map((item) -> {
            SkuLockVo skuLockVo = new SkuLockVo();
            skuLockVo.setSkuId(item.getSkuId());
            skuLockVo.setNum(item.getCount());
            //??????????????????????????????????????????
            List<Long> wareIds = baseMapper.listWareIdsHasStock(item.getSkuId(), item.getCount());
            skuLockVo.setWareIds(wareIds);
            return skuLockVo;
        }).collect(Collectors.toList());

        for(SkuLockVo vo : lockVos){
            boolean lock = true;
            Long skuId = vo.getSkuId();
            List<Long> wareIds = vo.getWareIds();
            //????????????????????????????????????????????????
            if (wareIds == null || wareIds.size() == 0){
                throw new NoStockException(skuId);
            }
            else{
                for (Long wareId : wareIds) {
                    Long count = baseMapper.lockWareSku(skuId, vo.getNum(), wareId);
                    if (count == 0){
                        lock = false; //???????????????????????? ???????????????????????????
                    }
                    else{
                        //?????????????????????????????????
                        WareOrderTaskDetailEntity wareOrderTaskDetailEntity = new WareOrderTaskDetailEntity();
                        wareOrderTaskDetailEntity.setLockStatus(1);
                        wareOrderTaskDetailEntity.setSkuNum(vo.getNum());
                        wareOrderTaskDetailEntity.setSkuId(vo.getSkuId());
                        wareOrderTaskDetailEntity.setTaskId(wareOrderTaskEntity.getId());
                        wareOrderTaskDetailEntity.setWareId(wareId);
                        wareOrderTaskDetailService.save(wareOrderTaskDetailEntity);
                        //??????????????????
                        StockLockedTo stockLockedTo = new StockLockedTo();
                        stockLockedTo.setId(wareOrderTaskEntity.getId());
                        StockDetailTo stockDetailTo = new StockDetailTo();
                        BeanUtils.copyProperties(wareOrderTaskDetailEntity, stockDetailTo);
                        stockLockedTo.setDetailTo(stockDetailTo);
                        rabbitTemplate.convertAndSend("ware-event-exchange", "ware.create.ware", stockLockedTo);

                        lock = true;
                        break;
                    }
                }
                //????????????????????????
                if(lock == false){
                    throw  new NoStockException(skuId);
                }
            }
        }
        return true;
    }
    @Data
    class SkuLockVo{
        private Long skuId;
        private Integer num;
        private List<Long> wareIds;
    }

    /**
     *    1??????????????????????????????????????????
     *          *          2??????????????????????????????????????????
     *          *              ???????????????????????????????????????
     *          *                      ??????????????????????????????
     * ????????????????????????
     * @param stockLockedTo
     */
    @Override
    public void unlock(StockLockedTo stockLockedTo) {
        StockDetailTo detailTo = stockLockedTo.getDetailTo();
        WareOrderTaskDetailEntity detailEntity = wareOrderTaskDetailService.getById(detailTo.getId());
        //1.????????????????????????????????????????????????????????????
        if (detailEntity != null) {
            WareOrderTaskEntity taskEntity = wareOrderTaskService.getById(stockLockedTo.getId());
            R r = orderFeignService.infoByOrderSn(taskEntity.getOrderSn()); //?????????????????????
            if (r.getCode() == 0) {
                OrderTo order = r.getData("order", new TypeReference<OrderTo>() {
                });
                //??????????????????||???????????????????????? ????????????
                if (order == null||order.getStatus() == OrderStatusEnum.CANCLED.getCode()) {
                    //???????????????????????????????????????????????????????????????????????????????????????
                    if (detailEntity.getLockStatus()== WareTaskStatusEnum.Locked.getCode()){
                        unlockStock(detailTo.getSkuId(), detailTo.getSkuNum(), detailTo.getWareId(), detailEntity.getId());
                    }
                }
            }else {
                throw new RuntimeException("??????????????????????????????");
            }
        }else {
            //????????????
        }
    }

    /**
     * ?????????????????????????????????????????????
     * @param orderTo
     */
    @Transactional
    @Override
    public void unlock(OrderTo orderTo) {
        WareOrderTaskEntity orderTaskEntity = wareOrderTaskService.getOne(new QueryWrapper<WareOrderTaskEntity>()
                .eq("order_sn", orderTo.getOrderSn()));
        //???????????????????????????????????????
        List<WareOrderTaskDetailEntity> detailList = wareOrderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>()
                .eq("task_id", orderTaskEntity.getId()).eq("lock_status", 1));
        for(WareOrderTaskDetailEntity detail : detailList){
            unlockStock(detail.getSkuId(), detail.getSkuNum(), detail.getWareId(), detail.getId());
        }
    }


    private void unlockStock(Long skuId, Integer skuNum, Long wareId, Long detailId) {
        //??????????????????????????????
        baseMapper.unlockStock(skuId, skuNum, wareId);
        //????????????????????????????????????
        WareOrderTaskDetailEntity detail = new WareOrderTaskDetailEntity();
        detail.setId(detailId);
        detail.setLockStatus(2);
        wareOrderTaskDetailService.updateById(detail);
    }

}