package com.atsjh.gulimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atsjh.common.constant.ProductConstant;
import com.atsjh.common.utils.R;
import com.atsjh.gulimall.product.entity.*;
import com.atsjh.gulimall.product.es.SkuEsModel;
import com.atsjh.gulimall.product.feign.SearchFeignService;
import com.atsjh.gulimall.product.feign.WareFeignService;
import com.atsjh.gulimall.product.service.*;
import com.atsjh.gulimall.product.vo.SkuHasStochVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atsjh.common.utils.PageUtils;
import com.atsjh.common.utils.Query;

import com.atsjh.gulimall.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    SkuInfoService skuInfoService;

    @Autowired
    BrandService brandService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    ProductAttrValueService attrValueService;

    @Autowired
    AttrService attrService;

    @Autowired
    WareFeignService wareFeignService;

    @Autowired
    SearchFeignService searchFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 模糊查询SPU
     * status
     * 	0
     * key
     * 	sa
     * brandId
     * 	7
     * catelogId
     * 	225
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();
        String key = (String)params.get("key");
        String status = (String)params.get("status");
        String brandId = (String)params.get("brandId");
        String catelogId = (String)params.get("catelogId");

        if(!StringUtils.isEmpty(key)){
            wrapper.and(w->{
                w.eq("id", key).or().like("spu_name", key);
            });
        }
        if(!StringUtils.isEmpty(status)){
            wrapper.eq("publish_status", status);
        }
        if(!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(brandId)){
            wrapper.eq("brand_id", brandId);
        }
        if(!StringUtils.isEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)){
            wrapper.eq("catalog_id", catelogId);
        }

        IPage<SpuInfoEntity> page = this.page(new Query<SpuInfoEntity>().getPage(params), wrapper);
        return new PageUtils(page);
    }

    /**
     * 商品上架, 将所有sku的检索信息加入到es中
     * @param spuId
     */
    @Transactional
    @Override
    public void up(Long spuId) {
        //根据spuId查出所有sku,

        List<SkuInfoEntity> skuInfoEntities = skuInfoService.getSkuListBySpuId(spuId);

        // attrs 查出spu的attrs， 每个sku的attrs都相同
        List<ProductAttrValueEntity> attrValueEntities =  attrValueService.getBySpuId(spuId);
        List<Long> attrIds = attrValueEntities.stream().map(attrValue -> {
            return attrValue.getAttrId();
        }).collect(Collectors.toList());

        //过滤掉searchtype=0的attr
        List<Long> sreachAttrIds = attrService.getSreachAttrIds(attrIds);
        HashSet<Long> set = new HashSet<>(sreachAttrIds);

        List<SkuEsModel.Attr> attrs = attrValueEntities.stream().filter(attr -> {
            return set.contains(attr.getAttrId());
        }).map(attr -> {
            SkuEsModel.Attr attr1 = new SkuEsModel.Attr();
            BeanUtils.copyProperties(attr, attr1);
            return attr1;
        }).collect(Collectors.toList());

        log.info("attrs查询完成");

        //调用ware服务查出所有sku的库存
        List<Long> skuIds = skuInfoEntities.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());

        Map<Long, Boolean> skuStocks = null;
        try {
            R r = wareFeignService.hasStock(skuIds);
            TypeReference<List<SkuHasStochVo>> typeReference = new TypeReference<List<SkuHasStochVo>>() {};
            List<SkuHasStochVo> data = (List<SkuHasStochVo>)r.getData(typeReference);
            skuStocks = data.stream().collect(Collectors.toMap(SkuHasStochVo::getSkuId, SkuHasStochVo::getHasStock));
        } catch (Exception e) {
            log.error("ware远程调用出现问题:{}", e);
        }
        log.info("ware远程查询库存完成");

        Map<Long, Boolean> finalSkuStocks = skuStocks;
        List<SkuEsModel> esModels = skuInfoEntities.stream().map(sku -> {
            SkuEsModel skuEsModel = new SkuEsModel();
            BeanUtils.copyProperties(sku, skuEsModel);
            //skuPrice skuImg
            skuEsModel.setSkuImg(sku.getSkuDefaultImg());
            skuEsModel.setSkuPrice(sku.getPrice());

            // hasStock hotScore
            if(finalSkuStocks == null){
                skuEsModel.setHasStock(true);
            }
            else{
                skuEsModel.setHasStock(finalSkuStocks.get(sku.getSkuId()));
            }
            skuEsModel.setHotScore(0L);

            // brandName brandImg
            BrandEntity byId = brandService.getById(sku.getBrandId());
            skuEsModel.setBrandName(byId.getName());
            skuEsModel.setBrandImg(byId.getLogo());

            // catalogName
            CategoryEntity byId1 = categoryService.getById(sku.getCatalogId());
            skuEsModel.setCatalogName(byId1.getName());

            skuEsModel.setAttrs(attrs);

            return skuEsModel;
        }).collect(Collectors.toList());

        log.info("esModels封装完成");

        //交给es
        R r = searchFeignService.saveProduct(esModels);
        if(r.getCode() == 0){
            //上架成功, 修改spu的上架状态
            this.baseMapper.updateSpuStatus(spuId, ProductConstant.StatusEnum.SPU_UP.getCode());
            log.info("商品上架成功");
        }
        else {
            // TODO 重复调用？ 接口幂等性， 重试机制
        }
    }

}