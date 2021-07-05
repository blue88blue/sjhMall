package com.atsjh.gulimall.coupon.dao;

import com.atsjh.gulimall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author jiahuansong
 * @email jiahuansong@qq.com
 * @date 2021-06-08 14:32:39
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
