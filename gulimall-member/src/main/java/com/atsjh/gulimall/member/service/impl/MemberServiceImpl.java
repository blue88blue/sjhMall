package com.atsjh.gulimall.member.service.impl;

import com.atsjh.gulimall.member.dao.MemberLevelDao;
import com.atsjh.gulimall.member.entity.MemberLevelEntity;
import com.atsjh.gulimall.member.exception.PhoneUniqueException;
import com.atsjh.gulimall.member.exception.UserNameUniqueException;
import com.atsjh.gulimall.member.vo.MemberRegistVo;
import com.atsjh.gulimall.member.vo.UserLoginVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
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
     * 用户登录， 匹配密码
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

    private void checkUserNameUnique(String userName) throws UserNameUniqueException {
        Integer count = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", userName));
        if(count > 0){
            throw new UserNameUniqueException();
        }

    }

    private void checkPhoneUnique(String phone) throws PhoneUniqueException {
        Integer count = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if(count > 0){
            throw new PhoneUniqueException();
        }
    }

}