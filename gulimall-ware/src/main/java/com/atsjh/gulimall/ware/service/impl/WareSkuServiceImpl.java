package com.atsjh.gulimall.ware.service.impl;

import com.atsjh.common.utils.R;
import com.atsjh.gulimall.ware.feign.ProductFeignService;
import com.atsjh.gulimall.ware.vo.SkuHasStochVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    ProductFeignService productFeignService;

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

}