package com.atsjh.gulimall.ware.service.impl;

import com.atsjh.common.constant.WareConstant;
import com.atsjh.gulimall.ware.entity.PurchaseDetailEntity;
import com.atsjh.gulimall.ware.service.PurchaseDetailService;
import com.atsjh.gulimall.ware.service.WareSkuService;
import com.atsjh.gulimall.ware.vo.MergeVo;
import com.atsjh.gulimall.ware.vo.PurchaseDoneVo;
import com.atsjh.gulimall.ware.vo.PurchaseItemDoneVo;
import org.omg.CORBA.IRObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atsjh.common.utils.PageUtils;
import com.atsjh.common.utils.Query;

import com.atsjh.gulimall.ware.dao.PurchaseDao;
import com.atsjh.gulimall.ware.entity.PurchaseEntity;
import com.atsjh.gulimall.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.soap.DetailEntry;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {
    @Autowired
    PurchaseDetailService purchaseDetailService;
    @Autowired
    WareSkuService skuService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageUnreceiveList(Map<String, Object> params) {
        QueryWrapper<PurchaseEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "0").or().eq("status", "1");

        IPage<PurchaseEntity> page = this.page(new Query<PurchaseEntity>().getPage(params), wrapper);
        return new PageUtils(page);

    }

    @Transactional
    @Override
    public void mergePurchase(MergeVo vo) {
        Long purchaseId = vo.getPurchaseId();
        if(purchaseId == null){
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setCreateTime(new Date());
            purchaseEntity.setUpdateTime(new Date());
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATED.getCode());
            this.baseMapper.insert(purchaseEntity);
            purchaseId = purchaseEntity.getId();
        }
        //TODO PurchaseDetailEntity的状态要为0或1才能合并

        List<Long> items = vo.getItems();
        Long finalPurchaseId = purchaseId;
        List<PurchaseDetailEntity> list = items.stream().map(i -> {
            PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
            purchaseDetailEntity.setId(i);
            purchaseDetailEntity.setPurchaseId(finalPurchaseId);
            purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());
            return purchaseDetailEntity;
        }).collect(Collectors.toList());
        purchaseDetailService.updateBatchById(list);

        //更新采购单的更新时间
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(finalPurchaseId);
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);
    }

    @Override
    public void receivedPurchase(List<Long> items) {
        //更新采购单状态， 更新时间， 确认确认采购单是新建或者已分配状态的
        List<PurchaseEntity> collect = items.stream().map(i -> {
            PurchaseEntity byId = this.getById(i);
            return byId;
        }).filter(item -> { //过滤 确认采购单是新建或者已分配状态的
            if (item.getStatus() == WareConstant.PurchaseStatusEnum.CREATED.getCode()
                    || item.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode()) {
                return true;
            }
            return false;
        }).map(item -> {  //更改状态与时间
            item.setStatus(WareConstant.PurchaseStatusEnum.RECIVED.getCode());
            item.setUpdateTime(new Date());
            return item;
        }).collect(Collectors.toList());
        this.updateBatchById(collect);

        //更新各个采购单中的PurchaseDetailEntity的状态
        collect.forEach( item ->{
            List<PurchaseDetailEntity> list = purchaseDetailService.getPurchaseDetailByPurchase(item.getId());
            List<PurchaseDetailEntity> collect1 = list.stream().map(i -> {
                PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
                purchaseDetailEntity.setId(i.getId());
                purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.BUYING.getCode());
                return purchaseDetailEntity;
            }).collect(Collectors.toList());
            purchaseDetailService.updateBatchById(collect1);
        });
    }

    @Transactional
    @Override
    public void donePurchase(PurchaseDoneVo vo) {
        //更新每个采购项状态， 增加到sku仓库中
        boolean finishFalg = true;  //采购成功的标志
        ArrayList<PurchaseDetailEntity> purchaseDetailEntities = new ArrayList<>();
        List<PurchaseItemDoneVo> items = vo.getItems();
        for(PurchaseItemDoneVo item : items){
            PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
            purchaseDetailEntity.setId(item.getItemId());
            purchaseDetailEntity.setStatus(item.getStatus());
            purchaseDetailEntities.add(purchaseDetailEntity);

            if(item.getStatus() == WareConstant.PurchaseDetailStatusEnum.ERROR.getCode()){
                finishFalg = false;
            }
            else{
                //该采购项采购成功， 更新库存
                PurchaseDetailEntity detailEntity = purchaseDetailService.getById(item.getItemId());
                skuService.addStock(detailEntity.getSkuId(), detailEntity.getWareId(), detailEntity.getSkuNum());
            }
        }
        purchaseDetailService.updateBatchById(purchaseDetailEntities);

        //更新采购单状态， 时间
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(vo.getId());
        purchaseEntity.setStatus(finishFalg ? WareConstant.PurchaseStatusEnum.FINISHED.getCode()
                : WareConstant.PurchaseStatusEnum.ERROR.getCode());
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);
    }


}