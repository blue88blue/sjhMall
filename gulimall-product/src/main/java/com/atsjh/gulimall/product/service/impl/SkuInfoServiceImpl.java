package com.atsjh.gulimall.product.service.impl;

import com.atsjh.gulimall.product.entity.SkuImagesEntity;
import com.atsjh.gulimall.product.entity.SpuInfoDescEntity;
import com.atsjh.gulimall.product.entity.SpuInfoEntity;
import com.atsjh.gulimall.product.es.SkuEsModel;
import com.atsjh.gulimall.product.service.*;
import com.atsjh.gulimall.product.vo.SkuItemSaleAttrVo;
import com.atsjh.gulimall.product.vo.SkuItemVo;
import com.atsjh.gulimall.product.vo.SpuItemAttrGroupVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atsjh.common.utils.PageUtils;
import com.atsjh.common.utils.Query;

import com.atsjh.gulimall.product.dao.SkuInfoDao;
import com.atsjh.gulimall.product.entity.SkuInfoEntity;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {
    @Autowired
    SkuImagesService imagesService;

    @Autowired
    SpuInfoDescService spuInfoDescService;

    @Autowired
    ProductAttrValueService productAttrValueService;

    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    ThreadPoolExecutor executor;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * sku模糊查询
     * key
     * 	aa
     * catelogId
     * 	225
     * brandId
     * 	1
     * min
     * 	12
     * max
     * 	5000
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SkuInfoEntity> wrapper = new QueryWrapper<>();
        String key = (String)params.get("key");
        String catelogId = (String)params.get("catelogId");
        String brandId = (String)params.get("brandId");
        String min = (String)params.get("min");
        String max = (String)params.get("max");
        if(!StringUtils.isEmpty(key)){
            wrapper.and( w ->{
               w.eq("sku_id", key).like("sku_name", key);
            });
        }
        if(!StringUtils.isEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)){
            wrapper.eq("catalog_id", catelogId);
        }
        if(!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(brandId)){
            wrapper.eq("brand_id", brandId);
        }
        if(!StringUtils.isEmpty(min)){
            try {
                BigDecimal bigDecimal = new BigDecimal(min);
                if(bigDecimal.compareTo(new BigDecimal("0")) == 1){
                    wrapper.ge("price", bigDecimal);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(!StringUtils.isEmpty(max)){
            try {
                BigDecimal bigDecimal = new BigDecimal(max);
                if(bigDecimal.compareTo(new BigDecimal("0")) == 1){
                    wrapper.le("price", bigDecimal);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        IPage<SkuInfoEntity> page = this.page(new Query<SkuInfoEntity>().getPage(params), wrapper);
        return new PageUtils(page);
    }

    @Override
    public List<SkuInfoEntity> getSkuListBySpuId(Long spuId) {
        QueryWrapper<SkuInfoEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("spu_id", spuId);
        List<SkuInfoEntity> list = this.list(wrapper);
        return list;
    }

    @Override
    public SkuItemVo item(Long skuId) throws ExecutionException, InterruptedException {
        SkuItemVo vo = new SkuItemVo();

        CompletableFuture<SkuInfoEntity> future = CompletableFuture.supplyAsync(() -> {
            //sku基本属性
            SkuInfoEntity infoEntity = getById(skuId);
            vo.setInfo(infoEntity);
            return infoEntity;
        }, executor);

        CompletableFuture<Void> saleFuture = future.thenAcceptAsync((res) -> {
            //3、获取spu的销售属性组合
            List<SkuItemSaleAttrVo> saleAttrBySpuId = skuSaleAttrValueService.getSaleAttrBySpuId(res.getSpuId());
            vo.setSaleAttr(saleAttrBySpuId);
        }, executor);

        CompletableFuture<Void> descFuture = future.thenAcceptAsync((res) -> {
            //4、获取spu的介绍
            SpuInfoDescEntity spuInfoDescEntity = spuInfoDescService.getById(res.getSpuId());
            vo.setDesc(spuInfoDescEntity);
        }, executor);

        CompletableFuture<Void> attrsFuture = future.thenAcceptAsync((res) -> {
            //5、获取spu的规格参数信息
            //获得属性分组以及各个分组下的属性和属性值
            List<SpuItemAttrGroupVo> attrsWithGroup = productAttrValueService.getAttrsWithGroup(res.getSpuId());
            vo.setGroupAttrs(attrsWithGroup);
        }, executor);

        CompletableFuture<Void> imageFuture = CompletableFuture.runAsync(() -> {
            //sku图片
            List<SkuImagesEntity> images = imagesService.getImagesBySkuId(skuId);
            vo.setImages(images);
        }, executor);

        CompletableFuture.allOf(saleFuture, descFuture, attrsFuture, imageFuture).get();
        return vo;
    }


}