package com.atsjh.gulimall.order.feign;

import com.atsjh.gulimall.order.vo.FareVo;
import com.atsjh.gulimall.order.vo.MemberAddressVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * @author: sjh
 * @date: 2021/7/11 上午11:37
 * @description:
 */
@FeignClient("gulimall-member")
public interface MemberFeignService {

    @GetMapping("/member/memberreceiveaddress/addressList/{memberId}")
    List<MemberAddressVo> getAddressList(@PathVariable("memberId") Long memberId);

    @GetMapping("/member/memberreceiveaddress/address/{address_id}")
    FareVo getAddressById(@PathVariable("address_id") Long address_id);
}
