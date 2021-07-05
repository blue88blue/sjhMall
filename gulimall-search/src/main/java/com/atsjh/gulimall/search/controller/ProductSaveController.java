package com.atsjh.gulimall.search.controller;

import com.atsjh.common.exception.BizCodeEnum;
import com.atsjh.common.utils.R;
import com.atsjh.gulimall.search.es.SkuEsModel;
import com.atsjh.gulimall.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * @author: sjh
 * @date: 2021/6/26 下午8:07
 * @description:
 */
@Slf4j
@RequestMapping("/search/save")
@RestController
public class ProductSaveController {
    @Autowired
    ProductSaveService saveService;

    @PostMapping("/product")
    public R saveProduct(@RequestBody List<SkuEsModel> skuEsModels){
        log.info("产品保存ElasticSearch....");
        boolean flag = false;
        try {
            flag = saveService.saveProduct(skuEsModels);
        } catch (IOException e) {
            log.error("ProductSaveController商品上架错误:{}", e);
            return R.error(BizCodeEnum.PRODUCT_UP_EXCEPTION.getCode(), BizCodeEnum.PRODUCT_UP_EXCEPTION.getMsg());
        }
        if(flag){
            log.info("产品已保存至ElasticSearch");
            return R.ok();
        }
        else
            return R.error(BizCodeEnum.PRODUCT_UP_EXCEPTION.getCode(), BizCodeEnum.PRODUCT_UP_EXCEPTION.getMsg());
    }
}
