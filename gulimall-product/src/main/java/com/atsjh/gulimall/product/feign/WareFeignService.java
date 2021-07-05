package com.atsjh.gulimall.product.feign;

import com.atsjh.common.utils.R;
import com.atsjh.gulimall.product.vo.SkuHasStochVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * @author: sjh
 * @date: 2021/6/26 下午7:21
 * @description:
 */
@FeignClient("gulimall-ware")
public interface WareFeignService {

    @RequestMapping("ware/waresku/hasStock")
    R hasStock(@RequestBody List<Long> skuIds);
}
