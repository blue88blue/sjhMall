package com.atsjh.gulimall.product.service.impl;

import com.atsjh.gulimall.product.entity.AttrEntity;
import com.atsjh.gulimall.product.service.AttrService;
import com.atsjh.gulimall.product.vo.AttrGroupWithAttrsVo;
import com.atsjh.gulimall.product.vo.AttrVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atsjh.common.utils.PageUtils;
import com.atsjh.common.utils.Query;

import com.atsjh.gulimall.product.dao.AttrGroupDao;
import com.atsjh.gulimall.product.entity.AttrGroupEntity;
import com.atsjh.gulimall.product.service.AttrGroupService;
import org.springframework.web.bind.annotation.GetMapping;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    AttrService attrService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Long catgoryId) {
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<AttrGroupEntity>();
        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            wrapper.and((obj)->{
                obj.eq("attr_group_id", key).or().like("attr_group_name", key);
            });
        }

        if(catgoryId == 0){
            IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params), wrapper);
            return new PageUtils(page);
        }
        else{
            wrapper.eq("catelog_id", catgoryId);
            IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params), wrapper);
            return new PageUtils(page);
        }
    }

    /**
     *
     * @param catelogId
     * @return
     */
    @Override
    public List<AttrGroupWithAttrsVo> getAttrGroupsWithAttrs(Long catelogId) {

        List<AttrGroupEntity> attrGroupEntities = this.baseMapper.selectList(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));
        List<AttrGroupWithAttrsVo> data = attrGroupEntities.stream().map((item) -> {
            AttrGroupWithAttrsVo attrGroupWithAttrsVo = new AttrGroupWithAttrsVo();
            BeanUtils.copyProperties(item, attrGroupWithAttrsVo);

            List<AttrEntity> relatedAttr = attrService.getRelatedAttr(item.getAttrGroupId());
            List<AttrVo> attrVos = relatedAttr.stream().map((a) -> {
                AttrVo attrVo = new AttrVo();
                BeanUtils.copyProperties(a, attrVo);
                return attrVo;
            }).collect(Collectors.toList());
            attrGroupWithAttrsVo.setAttrs(attrVos);
            return attrGroupWithAttrsVo;
        }).collect(Collectors.toList());

        return data;
    }


}