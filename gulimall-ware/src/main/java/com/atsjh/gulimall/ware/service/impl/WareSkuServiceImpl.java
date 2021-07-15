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
            // 远程查找skuName， 如果失败不影响整个事务
            //TODO 还可以用什么方法回滚？
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
     * 根据skuId查每个sku的库存
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
     * 库存锁定
     * @param lockVo
     * @return
     */
    @Transactional
    @Override
    public boolean orderLockStock(WareSkuLockVo lockVo) {
        //保存库存工作单
        WareOrderTaskEntity wareOrderTaskEntity = new WareOrderTaskEntity();
        wareOrderTaskEntity.setOrderSn(lockVo.getOrderSn());
        wareOrderTaskEntity.setCreateTime(new Date());
        wareOrderTaskService.save(wareOrderTaskEntity);

        List<OrderItemVo> locks = lockVo.getLocks();
        List<SkuLockVo> lockVos = locks.stream().map((item) -> {
            SkuLockVo skuLockVo = new SkuLockVo();
            skuLockVo.setSkuId(item.getSkuId());
            skuLockVo.setNum(item.getCount());
            //找出所有库存大于商品数的仓库
            List<Long> wareIds = baseMapper.listWareIdsHasStock(item.getSkuId(), item.getCount());
            skuLockVo.setWareIds(wareIds);
            return skuLockVo;
        }).collect(Collectors.toList());

        for(SkuLockVo vo : lockVos){
            boolean lock = true;
            Long skuId = vo.getSkuId();
            List<Long> wareIds = vo.getWareIds();
            //如果没有满足条件的仓库，抛出异常
            if (wareIds == null || wareIds.size() == 0){
                throw new NoStockException(skuId);
            }
            else{
                for (Long wareId : wareIds) {
                    Long count = baseMapper.lockWareSku(skuId, vo.getNum(), wareId);
                    if (count == 0){
                        lock = false; //当前仓库没库存， 继续检查下一个仓库
                    }
                    else{
                        //锁定成功保存工作单详情
                        WareOrderTaskDetailEntity wareOrderTaskDetailEntity = new WareOrderTaskDetailEntity();
                        wareOrderTaskDetailEntity.setLockStatus(1);
                        wareOrderTaskDetailEntity.setSkuNum(vo.getNum());
                        wareOrderTaskDetailEntity.setSkuId(vo.getSkuId());
                        wareOrderTaskDetailEntity.setTaskId(wareOrderTaskEntity.getId());
                        wareOrderTaskDetailEntity.setWareId(wareId);
                        wareOrderTaskDetailService.save(wareOrderTaskDetailEntity);
                        //发送消息队列
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
                //当前商品没有库存
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
     *    1、没有这个订单，必须解锁库存
     *          *          2、有这个订单，不一定解锁库存
     *          *              订单状态：已取消：解锁库存
     *          *                      已支付：不能解锁库存
     * 消息队列解锁库存
     * @param stockLockedTo
     */
    @Override
    public void unlock(StockLockedTo stockLockedTo) {
        StockDetailTo detailTo = stockLockedTo.getDetailTo();
        WareOrderTaskDetailEntity detailEntity = wareOrderTaskDetailService.getById(detailTo.getId());
        //1.如果工作单详情不为空，说明该库存锁定成功
        if (detailEntity != null) {
            WareOrderTaskEntity taskEntity = wareOrderTaskService.getById(stockLockedTo.getId());
            R r = orderFeignService.infoByOrderSn(taskEntity.getOrderSn()); //查询订单的状态
            if (r.getCode() == 0) {
                OrderTo order = r.getData("order", new TypeReference<OrderTo>() {
                });
                //没有这个订单||订单状态已经取消 解锁库存
                if (order == null||order.getStatus() == OrderStatusEnum.CANCLED.getCode()) {
                    //为保证幂等性，只有当工作单详情处于被锁定的情况下才进行解锁
                    if (detailEntity.getLockStatus()== WareTaskStatusEnum.Locked.getCode()){
                        unlockStock(detailTo.getSkuId(), detailTo.getSkuNum(), detailTo.getWareId(), detailEntity.getId());
                    }
                }
            }else {
                throw new RuntimeException("远程调用订单服务失败");
            }
        }else {
            //无需解锁
        }
    }

    /**
     * 处理订单模块发送的解锁库存消息
     * @param orderTo
     */
    @Transactional
    @Override
    public void unlock(OrderTo orderTo) {
        WareOrderTaskEntity orderTaskEntity = wareOrderTaskService.getOne(new QueryWrapper<WareOrderTaskEntity>()
                .eq("order_sn", orderTo.getOrderSn()));
        //查找库存工作单中没有解锁的
        List<WareOrderTaskDetailEntity> detailList = wareOrderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>()
                .eq("task_id", orderTaskEntity.getId()).eq("lock_status", 1));
        for(WareOrderTaskDetailEntity detail : detailList){
            unlockStock(detail.getSkuId(), detail.getSkuNum(), detail.getWareId(), detail.getId());
        }
    }


    private void unlockStock(Long skuId, Integer skuNum, Long wareId, Long detailId) {
        //数据库中解锁库存数据
        baseMapper.unlockStock(skuId, skuNum, wareId);
        //更新库存工作单详情的状态
        WareOrderTaskDetailEntity detail = new WareOrderTaskDetailEntity();
        detail.setId(detailId);
        detail.setLockStatus(2);
        wareOrderTaskDetailService.updateById(detail);
    }

}