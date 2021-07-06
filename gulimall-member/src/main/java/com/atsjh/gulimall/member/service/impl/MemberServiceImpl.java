package com.atsjh.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atsjh.gulimall.member.dao.MemberLevelDao;
import com.atsjh.gulimall.member.entity.MemberLevelEntity;
import com.atsjh.gulimall.member.exception.PhoneUniqueException;
import com.atsjh.gulimall.member.exception.UserNameUniqueException;
import com.atsjh.gulimall.member.utils.HttpUtils;
import com.atsjh.gulimall.member.vo.MemberRegistVo;
import com.atsjh.gulimall.member.vo.SocialUser;
import com.atsjh.gulimall.member.vo.UserLoginVo;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atsjh.common.utils.PageUtils;
import com.atsjh.common.utils.Query;

import com.atsjh.gulimall.member.dao.MemberDao;
import com.atsjh.gulimall.member.entity.MemberEntity;
import com.atsjh.gulimall.member.service.MemberService;

import javax.annotation.Resource;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Resource
    MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 用户注册， 检查手机、用户名是否存在，存在则抛出异常， 密码加密， 保存用户数据
     * @param memberRegistVo
     * @throws UserNameUniqueException
     * @throws PhoneUniqueException
     */
    @Override
    public void regist(MemberRegistVo memberRegistVo) throws UserNameUniqueException, PhoneUniqueException{
        MemberEntity memberEntity = new MemberEntity();

        //设置会员等级
        MemberLevelEntity defaultLevel = memberLevelDao.getDefaultLevel();
        memberEntity.setLevelId(defaultLevel.getId());

        //检查手机号和用户名是否重复
        checkPhoneUnique(memberRegistVo.getPhone());
        checkUserNameUnique(memberRegistVo.getUserName());

        memberEntity.setUsername(memberRegistVo.getUserName());
        memberEntity.setMobile(memberRegistVo.getPhone());

        //密码加密
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        String password = bCryptPasswordEncoder.encode(memberRegistVo.getPassword());
        memberEntity.setPassword(password);

        this.save(memberEntity);
    }

    /**
     * 用户登录， 匹配密码， 返回MemberEntity
     * @param vo
     * @return
     */
    @Override
    public MemberEntity login(UserLoginVo vo) {
        QueryWrapper<MemberEntity> wrapper = new QueryWrapper<MemberEntity>()
                .eq("username", vo.getLoginacct()).or().eq("mobile", vo.getLoginacct());
        MemberEntity memberEntity = this.getOne(wrapper);
        if(memberEntity == null){
            return null;
        }
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        boolean matches = passwordEncoder.matches(vo.getPassword(), memberEntity.getPassword());
        if(matches){
            return memberEntity;
        }
        else{
            return null;
        }
    }

    /**
     * gitee社交软件登录
     * 使用token查出用户socialUid，查看数据库中是否已存在
     * 存在： 更新token与过期时间， 返回用户信息
     * 不存在： 将gitee中有用的信息保存至数据库
     *
     * @param vo 社交服务器的token等信息
     * @return 返回用户信息
     */
    @Override
    public MemberEntity socialLogin(SocialUser vo) throws Exception {
        HashMap<String, String> querys = new HashMap<>();
        querys.put("access_token", vo.getAccess_token());
        HttpResponse response = HttpUtils.doGet("https://gitee.com", "/api/v5/user", "get", new HashMap<>(), querys);

        if(response.getStatusLine().getStatusCode() == 200){
            String s = EntityUtils.toString(response.getEntity());
            JSONObject jsonObject = JSON.parseObject(s);
            String socialUid = jsonObject.getString("id");
            MemberEntity memberEntity = this.getOne(new QueryWrapper<MemberEntity>().eq("social_uid", socialUid));
            //用户已存在， 更新token
            if(memberEntity != null){
                MemberEntity updateEntity = new MemberEntity();
                updateEntity.setAccessToken(vo.getAccess_token());
                updateEntity.setExpiresIn(vo.getExpires_in());
                updateEntity.setId(memberEntity.getId());
                this.updateById(updateEntity); //更新token
                memberEntity.setAccessToken(vo.getAccess_token());
                memberEntity.setExpiresIn(vo.getExpires_in());
                return memberEntity;
            }
            else{ //注册用户
                String username = jsonObject.getString("login");
                String nickName = jsonObject.getString("name");
                MemberEntity newEntity = new MemberEntity();
                newEntity.setUsername(username);
                newEntity.setNickname(nickName);
                newEntity.setAccessToken(vo.getAccess_token());
                newEntity.setExpiresIn(vo.getExpires_in());
                newEntity.setSocialUid(socialUid);
                this.save(newEntity);
                return newEntity;
            }
        }

        return null;
    }

    /**
     * 判断用户名是否存在
     * @param userName
     * @throws UserNameUniqueException
     */
    private void checkUserNameUnique(String userName) throws UserNameUniqueException {
        Integer count = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", userName));
        if(count > 0){
            throw new UserNameUniqueException();
        }

    }

    /**
     * 判断手机号是否存在
     * @param phone
     * @throws PhoneUniqueException
     */
    private void checkPhoneUnique(String phone) throws PhoneUniqueException {
        Integer count = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if(count > 0){
            throw new PhoneUniqueException();
        }
    }

}