package com.atsjh.gulimall.member.controller;

import java.util.Arrays;
import java.util.Map;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import com.atsjh.common.exception.BizCodeEnum;
import com.atsjh.gulimall.member.entity.MemberLevelEntity;
import com.atsjh.gulimall.member.exception.PhoneUniqueException;
import com.atsjh.gulimall.member.exception.UserNameUniqueException;
import com.atsjh.gulimall.member.feign.CouponFeignService;
import com.atsjh.gulimall.member.vo.MemberRegistVo;
import com.atsjh.gulimall.member.vo.SocialUser;
import com.atsjh.gulimall.member.vo.UserLoginVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import com.atsjh.gulimall.member.entity.MemberEntity;
import com.atsjh.gulimall.member.service.MemberService;
import com.atsjh.common.utils.PageUtils;
import com.atsjh.common.utils.R;



/**
 * 会员
 *
 * @author jiahuansong
 * @email jiahuansong@qq.com
 * @date 2021-06-08 14:59:01
 */
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;

    @Autowired
    CouponFeignService couponFeignService;


    @PostMapping("/social/login")
    public R socialLogin(@RequestBody SocialUser vo) throws Exception {
        MemberEntity entity = memberService.socialLogin(vo);
        if(entity != null){
            return R.ok().setData(entity);
        }
        else{
            return R.error(BizCodeEnum.PASSWORD_INVALID_EXCEPTION.getCode(), BizCodeEnum.PASSWORD_INVALID_EXCEPTION.getMsg());
        }
    }


    @PostMapping("/login")
    public R login(@RequestBody UserLoginVo vo){
        MemberEntity entity = memberService.login(vo);
        if(entity == null){
            return R.error(BizCodeEnum.PASSWORD_INVALID_EXCEPTION.getCode(), BizCodeEnum.PASSWORD_INVALID_EXCEPTION.getMsg());
        }
        else{
            return R.ok().setData(entity);
        }
    }


    @PostMapping("/memberRegist")
    public R memberRegist(@RequestBody MemberRegistVo memberRegistVo){
        try {
            memberService.regist(memberRegistVo);
        } catch (UserNameUniqueException e) {
            return R.error(BizCodeEnum.USERNAME_EXIST_EXCEPTION.getCode(), BizCodeEnum.USERNAME_EXIST_EXCEPTION.getMsg());
        } catch (PhoneUniqueException e){
            return R.error(BizCodeEnum.PHONE_EXIST_EXCEPTION.getCode(), BizCodeEnum.PHONE_EXIST_EXCEPTION.getMsg());
        }
        return R.ok();
    }


    @RequestMapping("coupons")
    public R getMemberCoupons(){
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setNickname("张三");
        R r = couponFeignService.memberCoupon();
        return R.ok().put("member", memberEntity).put("coupon", r.get("coupon"));
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("member:member:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("member:member:info")
    public R info(@PathVariable("id") Long id){
		MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("member:member:save")
    public R save(@RequestBody MemberEntity member){
		memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("member:member:update")
    public R update(@RequestBody MemberEntity member){
		memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("member:member:delete")
    public R delete(@RequestBody Long[] ids){
		memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
