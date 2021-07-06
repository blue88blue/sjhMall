package com.atsjh.gulimall.authserver.feign;

import com.atsjh.common.utils.R;
import com.atsjh.gulimall.authserver.vo.SocialUser;
import com.atsjh.gulimall.authserver.vo.UserLoginVo;
import com.atsjh.gulimall.authserver.vo.UserRegistVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author: sjh
 * @date: 2021/7/6 上午11:04
 * @description:
 */
@FeignClient("gulimall-member")
public interface MemberFeignService {

    @PostMapping("/member/member/memberRegist")
    R memberRegist(@RequestBody UserRegistVo memberRegistVo);

    @PostMapping("/member/member/login")
    R login(@RequestBody UserLoginVo vo);

    @PostMapping("/member/member/social/login")
    R socialLogin(@RequestBody SocialUser vo);
}
