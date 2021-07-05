package com.atsjh.gulimall.product.feign;

import com.atsjh.common.to.SkuReductionTo;
import com.atsjh.common.to.SpuBoundsTo;
import com.atsjh.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author: sjh
 * @date: 2021/6/22 下午6:05
 * @description:
 */
@FeignClient("gulimall-coupon")
public interface CouponFeignService {

    @PostMapping("/coupon/spubounds/save")
    public R spuBoundSave(@RequestBody SpuBoundsTo spuBoundsTo);

    @PostMapping("/coupon/skufullreduction/saveSkuReduction")
    public R saveSkuReduction(SkuReductionTo skuReductionTo);
}
