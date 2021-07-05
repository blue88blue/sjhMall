package com.atsjh.gulimall.product.service.impl;

import com.atsjh.common.constant.ProductConstant;
import com.atsjh.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.atsjh.gulimall.product.dao.AttrGroupDao;
import com.atsjh.gulimall.product.dao.CategoryDao;
import com.atsjh.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.atsjh.gulimall.product.entity.AttrGroupEntity;
import com.atsjh.gulimall.product.entity.CategoryEntity;
import com.atsjh.gulimall.product.service.AttrAttrgroupRelationService;
import com.atsjh.gulimall.product.service.CategoryService;
import com.atsjh.gulimall.product.vo.AttrRespVo;
import com.atsjh.gulimall.product.vo.AttrVo;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atsjh.common.utils.PageUtils;
import com.atsjh.common.utils.Query;

import com.atsjh.gulimall.product.dao.AttrDao;
import com.atsjh.gulimall.product.entity.AttrEntity;
import com.atsjh.gulimall.product.service.AttrService;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {

    @Autowired
    AttrAttrgroupRelationService attrAttrgroupRelationService;

    @Resource
    AttrAttrgroupRelationDao attrAttrgroupRelationDao;

    @Resource
    AttrGroupDao attrGroupDao;

    @Resource
    CategoryDao categoryDao;

    @Autowired
    CategoryService categoryService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void saveDetail(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity);
        //保存基本数据
        this.save(attrEntity);
        //保存关联数据
        if(attr.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() && attr.getCatelogId() != null){
            AttrAttrgroupRelationEntity attrAttrgroupRelationEntity = new AttrAttrgroupRelationEntity();
            attrAttrgroupRelationEntity.setAttrGroupId(attr.getAttrGroupId());
            attrAttrgroupRelationEntity.setAttrId(attrEntity.getAttrId());
            attrAttrgroupRelationService.save(attrAttrgroupRelationEntity);
        }
    }

    @Override
    public PageUtils queryBaseList(Map<String, Object> params, Long categoryId, String type) {
        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<AttrEntity>()
                .eq("attr_type", "base".equalsIgnoreCase(type) ? ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() : ProductConstant.AttrEnum.ATTR_TYPE_SALE.getCode());
        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            wrapper.eq("attr_id", key).or().like("attr_name", key);
        }

        if(categoryId != 0){
            wrapper.eq("catelog_id", categoryId);
        }

        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), wrapper);

        PageUtils pageUtils = new PageUtils(page);
        List<AttrEntity> records = page.getRecords();

        List<AttrRespVo> attrRespList = records.stream().map((item) -> {
            AttrRespVo attrRespVo = new AttrRespVo();
            BeanUtils.copyProperties(item, attrRespVo);
            //设置分类和分组的名字
            //查询与分组的关联记录
            if("base".equalsIgnoreCase(type)){
                AttrAttrgroupRelationEntity attr_id = attrAttrgroupRelationDao.selectOne(
                        new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", item.getAttrId()));
                if (attr_id != null) {
                    AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attr_id.getAttrGroupId());//查询分组
                    attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }

            CategoryEntity categoryEntity = categoryDao.selectById(item.getCatelogId());
            if (categoryEntity != null) {
                attrRespVo.setCatelogName(categoryEntity.getName());
            }
            return attrRespVo;
        }).collect(Collectors.toList());

        pageUtils.setList(attrRespList);
        return pageUtils;
    }

    @Override
    public AttrRespVo getAttrInfo(Long attrId) {
        AttrRespVo attrRespVo = new AttrRespVo();
        AttrEntity attrEntity = this.getById(attrId);
        BeanUtils.copyProperties(attrEntity, attrRespVo);
        //封装分组与分类，以及路径

        AttrAttrgroupRelationEntity attr_id = attrAttrgroupRelationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrEntity.getAttrId()));
        if(attr_id != null){
            attrRespVo.setAttrGroupId(attr_id.getAttrGroupId());
            AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attr_id.getAttrGroupId());
            if(attrGroupEntity != null)
                attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
        }
        CategoryEntity categoryEntity = categoryDao.selectById(attrRespVo.getCatelogId());
        if(categoryEntity != null){
            attrRespVo.setCatelogId(categoryEntity.getCatId());
            attrRespVo.setCatelogName(categoryEntity.getName());
            Long[] logPath = categoryService.getLogPath(categoryEntity.getCatId());
            attrRespVo.setCatelogPath(logPath);
        }
        return attrRespVo;
    }

    @Transactional
    @Override
    public void updateDetail(AttrRespVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity);
        this.updateById(attrEntity);

        if(attr.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()){
            //更新关联数据
            AttrAttrgroupRelationEntity attrAttrgroupRelationEntity = new AttrAttrgroupRelationEntity();
            attrAttrgroupRelationEntity.setAttrGroupId(attr.getAttrGroupId());
            attrAttrgroupRelationEntity.setAttrId(attr.getAttrId());

            Integer count = attrAttrgroupRelationDao.selectCount(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attr.getAttrId()));
            if(count > 0){
                attrAttrgroupRelationDao.update(attrAttrgroupRelationEntity,new UpdateWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attr.getAttrId()));
            }
            else{
                attrAttrgroupRelationDao.insert(attrAttrgroupRelationEntity);
            }
        }

    }

    /**
     * 根据分组id查找与其关联的属性
     * @param attrGroupId
     * @return
     */
    @Override
    public List<AttrEntity> getRelatedAttr(Long attrGroupId) {
        //获取到关联数据
        List<AttrAttrgroupRelationEntity> list = attrAttrgroupRelationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", attrGroupId));
        //映射成id
        List<Long> ids = list.stream().map((a) -> {
            return a.getAttrId();
        }).collect(Collectors.toList());

        List<AttrEntity> attrEntities = this.listByIds(ids);
        return attrEntities;
    }

    @Override
    public PageUtils geNoAttrRelation(Map<String, Object> params, Long attrGroupId) {
        AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrGroupId);
        Long catelogId = attrGroupEntity.getCatelogId();

        //本分组中的attr
        List<AttrAttrgroupRelationEntity> relation = attrAttrgroupRelationDao.selectList(
                new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", attrGroupId));
        List<Long> attrIds = relation.stream().map((item) -> {
            return item.getAttrId();
        }).collect(Collectors.toList());

        //模糊查询
        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<>();
        String key = (String)params.get("key");
        if(!StringUtils.isEmpty(key)){
            wrapper.eq("attr_id", key).or().like("attr_name", key);
        }

        //查找该三级分类中的attr， 且不包含本分组中的
        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), wrapper.eq("catelog_id", catelogId).notIn("attr_id", attrIds));
        return new PageUtils(page);
    }

    @Override
    public List<Long> getSreachAttrIds(List<Long> attrIds) {

        return this.baseMapper.getSreachAttrIds(attrIds);
    }


}