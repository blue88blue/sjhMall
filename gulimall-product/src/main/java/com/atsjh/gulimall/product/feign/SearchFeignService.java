package com.atsjh.gulimall.product.feign;

import com.atsjh.common.utils.R;
import com.atsjh.gulimall.product.es.SkuEsModel;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author: sjh
 * @date: 2021/6/26 下午8:35
 * @description:
 */
@FeignClient("gulimall-search")
public interface SearchFeignService {
    @PostMapping("/search/save/product")
    public R saveProduct(@RequestBody List<SkuEsModel> skuEsModels);
}
