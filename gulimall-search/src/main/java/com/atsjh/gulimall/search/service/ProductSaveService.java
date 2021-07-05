package com.atsjh.gulimall.search.service;

import com.atsjh.gulimall.search.es.SkuEsModel;

import java.io.IOException;
import java.util.List;

/**
 * @author: sjh
 * @date: 2021/6/26 下午8:12
 * @description:
 */
public interface ProductSaveService {
    boolean saveProduct(List<SkuEsModel> skuEsModels) throws IOException;
}
