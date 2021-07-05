package com.atsjh.gulimall.order.dao;

import com.atsjh.gulimall.order.entity.OrderSettingEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单配置信息
 * 
 * @author jiahuansong
 * @email jiahuansong@qq.com
 * @date 2021-06-08 15:02:09
 */
@Mapper
public interface OrderSettingDao extends BaseMapper<OrderSettingEntity> {
	
}
