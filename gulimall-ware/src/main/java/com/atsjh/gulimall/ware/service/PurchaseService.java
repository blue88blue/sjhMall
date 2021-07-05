package com.atsjh.gulimall.ware.service;

import com.atsjh.gulimall.ware.vo.MergeVo;
import com.atsjh.gulimall.ware.vo.PurchaseDoneVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atsjh.common.utils.PageUtils;
import com.atsjh.gulimall.ware.entity.PurchaseEntity;

import java.util.List;
import java.util.Map;

/**
 * 采购信息
 *
 * @author jiahuansong
 * @email jiahuansong@qq.com
 * @date 2021-06-08 15:08:25
 */
public interface PurchaseService extends IService<PurchaseEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPageUnreceiveList(Map<String, Object> params);


    void mergePurchase(MergeVo vo);

    void receivedPurchase(List<Long> items);

    void donePurchase(PurchaseDoneVo vo);
}

