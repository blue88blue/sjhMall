package com.atsjh.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.atsjh.gulimall.search.constant.Esconstant;
import com.atsjh.gulimall.search.config.ElasticSearchConfig;
import com.atsjh.gulimall.search.es.SkuEsModel;
import com.atsjh.gulimall.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: sjh
 * @date: 2021/6/26 下午8:12
 * @description:
 */
@Slf4j
@Service
public class ProductSaveServiceImpl implements ProductSaveService {
    @Autowired
    RestHighLevelClient restHighLevelClient;

    /**
     * 将sku保存到es中
     * @param skuEsModels
     * @return
     * @throws IOException
     */
    @Override
    public boolean saveProduct(List<SkuEsModel> skuEsModels) throws IOException {
        //首先要建立好索引
        //给es中保存这些数据
        BulkRequest bulkRequest = new BulkRequest();
        for(SkuEsModel vo: skuEsModels){
            //构造保存请求
            IndexRequest indexRequest = new IndexRequest(Esconstant.PRODUCT_INDEX);
            indexRequest.id(vo.getSkuId().toString());
            String jsonString = JSON.toJSONString(vo);
            indexRequest.source(jsonString, XContentType.JSON);

            bulkRequest.add(indexRequest);
        }
        BulkResponse response = restHighLevelClient.bulk(bulkRequest, ElasticSearchConfig.COMMON_OPTIONS);

        // TODO 如果出现错误
        boolean b = response.hasFailures();
        BulkItemResponse[] items = response.getItems();
        List<Integer> collect = Arrays.stream(items).map(item -> {
            return item.getItemId();
        }).collect(Collectors.toList());
        log.info("商品上架成功:{}",collect);

        return !b;
    }
}
