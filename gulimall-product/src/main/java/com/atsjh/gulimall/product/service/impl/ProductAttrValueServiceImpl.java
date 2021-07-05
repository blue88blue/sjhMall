package com.atsjh.gulimall.product.service.impl;

import com.atsjh.gulimall.product.vo.SpuItemAttrGroupVo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atsjh.common.utils.PageUtils;
import com.atsjh.common.utils.Query;

import com.atsjh.gulimall.product.dao.ProductAttrValueDao;
import com.atsjh.gulimall.product.entity.ProductAttrValueEntity;
import com.atsjh.gulimall.product.service.ProductAttrValueService;


@Service("productAttrValueService")
public class ProductAttrValueServiceImpl extends ServiceImpl<ProductAttrValueDao, ProductAttrValueEntity> implements ProductAttrValueService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<ProductAttrValueEntity> page = this.page(
                new Query<ProductAttrValueEntity>().getPage(params),
                new QueryWrapper<ProductAttrValueEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<ProductAttrValueEntity> getBySpuId(Long spuId) {
        QueryWrapper<ProductAttrValueEntity> wrapper = new QueryWrapper<ProductAttrValueEntity>().eq("spu_id", spuId);
        List<ProductAttrValueEntity> list = this.list(wrapper);
        return list;
    }

    @Override
    public List<SpuItemAttrGroupVo> getAttrsWithGroup(Long spuId) {
        //连表查询spu的每个属性，以及属性的分组
        return this.baseMapper.getAttrsWithGroup(spuId);
    }

}