package com.atsjh.gulimall.ware.service;

import com.atsjh.common.utils.PageUtils;
import com.atsjh.gulimall.ware.entity.PurchaseDetailEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 *
 *
 * @author zsy
 * @email 594983498@qq.com
 * @date 2019-11-17 13:50:10
 */
public interface PurchaseDetailService extends IService<PurchaseDetailEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<PurchaseDetailEntity> listDetailByPurchaseId(Long id);


    List<PurchaseDetailEntity> getPurchaseDetailByPurchase(Long id);
}

