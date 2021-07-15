package com.atsjh.gulimall.order.feign;

import com.atsjh.common.utils.R;
import lombok.experimental.FieldDefaults;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author: sjh
 * @date: 2021/7/12 下午2:15
 * @description:
 */
@FeignClient("gulimall-product")
public interface ProductFeignService {

    @GetMapping("/product/spuinfo/getSpu/{skuId}")
    R getSpuBySkuId(@PathVariable("skuId") Long skuId);
}
