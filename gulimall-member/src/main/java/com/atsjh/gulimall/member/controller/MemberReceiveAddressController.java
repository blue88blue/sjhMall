package com.atsjh.gulimall.member.controller;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import com.atsjh.gulimall.member.vo.FareVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.atsjh.gulimall.member.entity.MemberReceiveAddressEntity;
import com.atsjh.gulimall.member.service.MemberReceiveAddressService;
import com.atsjh.common.utils.PageUtils;
import com.atsjh.common.utils.R;



/**
 * 会员收货地址
 *
 * @author jiahuansong
 * @email jiahuansong@qq.com
 * @date 2021-06-08 14:59:01
 */
@RestController
@RequestMapping("member/memberreceiveaddress")
public class MemberReceiveAddressController {
    @Autowired
    private MemberReceiveAddressService memberReceiveAddressService;

    @GetMapping("addressList/{memberId}")
    public List<MemberReceiveAddressEntity> getAddressList(@PathVariable("memberId") Long memberId){
        return memberReceiveAddressService.getAddressList(memberId);
    }

    @GetMapping("address/{address_id}")
    @ResponseBody
    public FareVo getAddressById(@PathVariable("address_id") Long address_id){
        MemberReceiveAddressEntity byId = memberReceiveAddressService.getById(address_id);
        FareVo fareVo = new FareVo();
        fareVo.setAddress(byId);
        fareVo.setFare(new BigDecimal(6.5));
        return fareVo;
    }


    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("member:memberreceiveaddress:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = memberReceiveAddressService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("member:memberreceiveaddress:info")
    public R info(@PathVariable("id") Long id){
		MemberReceiveAddressEntity memberReceiveAddress = memberReceiveAddressService.getById(id);

        return R.ok().put("memberReceiveAddress", memberReceiveAddress);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("member:memberreceiveaddress:save")
    public R save(@RequestBody MemberReceiveAddressEntity memberReceiveAddress){
		memberReceiveAddressService.save(memberReceiveAddress);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("member:memberreceiveaddress:update")
    public R update(@RequestBody MemberReceiveAddressEntity memberReceiveAddress){
		memberReceiveAddressService.updateById(memberReceiveAddress);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("member:memberreceiveaddress:delete")
    public R delete(@RequestBody Long[] ids){
		memberReceiveAddressService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
