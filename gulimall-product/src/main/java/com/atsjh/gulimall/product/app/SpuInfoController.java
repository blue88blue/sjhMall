package com.atsjh.gulimall.product.app;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import com.atsjh.common.to.SkuReductionTo;
import com.atsjh.common.to.SpuBoundsTo;
import com.atsjh.gulimall.product.entity.*;
import com.atsjh.gulimall.product.feign.CouponFeignService;
import com.atsjh.gulimall.product.service.*;
import com.atsjh.gulimall.product.vo.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.atsjh.common.utils.PageUtils;
import com.atsjh.common.utils.R;


/**
 * spu信息
 *
 * @author jiahuansong
 * @email jiahuansong@qq.com
 * @date 2021-06-07 21:37:42
 */
@RestController
@RequestMapping("product/spuinfo")
public class SpuInfoController {
    @Autowired
    private SpuInfoService spuInfoService;

    @Autowired
    SpuInfoDescService spuInfoDescService;

    @Autowired
    SpuImagesService spuImagesService;

    @Autowired
    ProductAttrValueService productAttrValueService;

    @Autowired
    AttrService attrService;

    @Autowired
    SkuInfoService skuInfoService;

    @Autowired
    SkuImagesService skuImagesService;

    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    CouponFeignService couponFeignService;

    /**
     * /product/spuinfo/{spuId}/up
     * 商品上架
     */
    @RequestMapping("/{spuId}/up")
    public R up(@PathVariable("spuId") Long spuId){
        spuInfoService.up(spuId);
        return R.ok();
    }


    /**
     * TODO 高级部分优化， 处理失败情况
     */
    @Transactional
    @RequestMapping("/save")
    public R saveSpuInfo(@RequestBody SpuSaveVo vo) {
        System.out.println("保存SPU info");
        System.out.println(vo);
        // 1.保存spu基本信息 pms_sku_info
        SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(vo, spuInfoEntity);
        spuInfoEntity.setCreateTime(new Date());
        spuInfoEntity.setUpdateTime(new Date());
        spuInfoEntity.setSpuName(vo.getSpuName());
        spuInfoService.save(spuInfoEntity);

        System.out.println("保存spu基本信息");

        // 2.保存spu的表述图片  pms_spu_info_desc
        List<String> decript = vo.getDecript();
        System.out.println(spuInfoEntity.getId()+"********"+decript);
        SpuInfoDescEntity spuInfoDescEntity = new SpuInfoDescEntity();
        spuInfoDescEntity.setSpuId(spuInfoEntity.getId());
        spuInfoDescEntity.setDecript(String.join(",", decript));
        spuInfoDescService.save(spuInfoDescEntity);
        System.out.println("保存spu的表述图片");

        // 3.保存spu的图片集  pms_sku_images
        List<String> images = vo.getImages();
        spuImagesService.saveImages(spuInfoEntity.getId(), images);
        System.out.println("保存spu的图片集");

        // 4.保存spu的规格属性  pms_product_attr_value
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
        List<ProductAttrValueEntity> collect = baseAttrs.stream().map(attr -> {
            ProductAttrValueEntity productAttrValueEntity = new ProductAttrValueEntity();
            productAttrValueEntity.setAttrId(attr.getAttrId());
            AttrEntity byId = attrService.getById(attr.getAttrId());
            productAttrValueEntity.setAttrName(byId.getAttrName());
            productAttrValueEntity.setSpuId(spuInfoEntity.getId());
            productAttrValueEntity.setQuickShow(attr.getShowDesc());
            productAttrValueEntity.setAttrValue(attr.getAttrValues());
            return productAttrValueEntity;
        }).collect(Collectors.toList());
        productAttrValueService.saveBatch(collect);
        System.out.println("保存spu的规格属性");

        // 5.保存当前spu对应所有sku信息

        // 1).spu的积分信息 sms_spu_bounds
        Bounds bounds = vo.getBounds();
        SpuBoundsTo spuBoundsTo = new SpuBoundsTo();
        BeanUtils.copyProperties(bounds, spuBoundsTo);
        spuBoundsTo.setSpuId(spuInfoEntity.getId());
        R r = couponFeignService.spuBoundSave(spuBoundsTo);
        if(r.getCode() != 0){
            System.out.println("远程couponFeignService.spuBoundSave服务不成功");
        }


        // 2).基本信息的保存 pms_sku_info
        List<Skus> skus = vo.getSkus();
        List<SkuInfoEntity> skuInfoEntities = skus.stream().map(item -> {
            SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
            BeanUtils.copyProperties(item, skuInfoEntity);
            skuInfoEntity.setBrandId(vo.getBrandId());
            skuInfoEntity.setCatalogId(vo.getCatalogId());
            skuInfoEntity.setSaleCount(0L);
            skuInfoEntity.setSkuDesc(vo.getSpuDescription());
            skuInfoEntity.setSpuId(spuInfoEntity.getId());
            String delaultImage = "";
            for(Images image : item.getImages()){
                if(image.getDefaultImg() == 1){
                    delaultImage = image.getImgUrl();
                    break;
                }
            }
            skuInfoEntity.setSkuDefaultImg(delaultImage);
            return skuInfoEntity;
        }).collect(Collectors.toList());
        skuInfoService.saveBatch(skuInfoEntities);


        for(int i=0; i<skus.size(); i++){
            Skus sku = skus.get(i);
            // 3).保存sku的图片信息  pms_sku_images
            List<Images> sku_images = sku.getImages();
            Long skuId = skuInfoEntities.get(i).getSkuId();
            List<SkuImagesEntity> skuImagesEntities = sku_images.stream().map(item -> {
                SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                BeanUtils.copyProperties(item, skuImagesEntity);
                skuImagesEntity.setSkuId(skuId);
                return skuImagesEntity;
            }).filter(item->{
                //返回true的被留下
                return !StringUtils.isEmpty(item.getImgUrl());
            }).collect(Collectors.toList());

            skuImagesService.saveBatch(skuImagesEntities);

            // 4).sku的销售属性  pms_sku_sale_attr_value
            List<Attr> attr = skus.get(i).getAttr();

            List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = attr.stream().map(item -> {
                SkuSaleAttrValueEntity skuSaleAttrValueEntity = new SkuSaleAttrValueEntity();
                BeanUtils.copyProperties(item, skuSaleAttrValueEntity);
                skuSaleAttrValueEntity.setSkuId(skuId);
                return skuSaleAttrValueEntity;
            }).collect(Collectors.toList());
            skuSaleAttrValueService.saveBatch(skuSaleAttrValueEntities);

            // 5.) sku的优惠、满减、会员价格等信息  [跨库]

            SkuReductionTo skuReductionTo = new SkuReductionTo();
            BeanUtils.copyProperties(sku, skuReductionTo);
            skuReductionTo.setSkuId(skuId);
            if(sku.getFullCount()>0 || sku.getFullPrice().compareTo(new BigDecimal("0")) == 1){//优惠有意义才保存
                R r1 = couponFeignService.saveSkuReduction(skuReductionTo);
                if(r1.getCode() != 0){
                    System.out.println("远程couponFeignService.saveSkuReduction服务不成功");
                }
            }
        }

        return R.ok();
    }

    @GetMapping("/getSpu/{skuId}")
    public R getSpuBySkuId(@PathVariable("skuId") Long skuId){
        SpuInfoEntity spu = spuInfoService.getSpuBySkuId(skuId);
        return R.ok().setData(spu);
    }



    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:spuinfo:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = spuInfoService.queryPageByCondition(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("product:spuinfo:info")
    public R info(@PathVariable("id") Long id){
		SpuInfoEntity spuInfo = spuInfoService.getById(id);

        return R.ok().put("spuInfo", spuInfo);
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:spuinfo:update")
    public R update(@RequestBody SpuInfoEntity spuInfo){
		spuInfoService.updateById(spuInfo);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:spuinfo:delete")
    public R delete(@RequestBody Long[] ids){
		spuInfoService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
