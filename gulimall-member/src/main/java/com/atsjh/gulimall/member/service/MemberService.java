package com.atsjh.gulimall.member.service;

import com.atsjh.gulimall.member.entity.MemberLevelEntity;
import com.atsjh.gulimall.member.vo.MemberRegistVo;
import com.atsjh.gulimall.member.vo.SocialUser;
import com.atsjh.gulimall.member.vo.UserLoginVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atsjh.common.utils.PageUtils;
import com.atsjh.gulimall.member.entity.MemberEntity;

import java.util.Map;

/**
 * 会员
 *
 * @author jiahuansong
 * @email jiahuansong@qq.com
 * @date 2021-06-08 14:59:01
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(MemberRegistVo memberRegistVo);

    MemberEntity login(UserLoginVo vo);

    MemberEntity socialLogin(SocialUser vo) throws Exception;
}

