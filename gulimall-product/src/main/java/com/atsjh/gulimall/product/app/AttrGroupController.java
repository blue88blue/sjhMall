package com.atsjh.gulimall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import com.atsjh.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.atsjh.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.atsjh.gulimall.product.entity.AttrEntity;
import com.atsjh.gulimall.product.service.AttrAttrgroupRelationService;
import com.atsjh.gulimall.product.service.AttrService;
import com.atsjh.gulimall.product.service.CategoryService;
import com.atsjh.gulimall.product.vo.AttrGroupWithAttrsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.atsjh.gulimall.product.entity.AttrGroupEntity;
import com.atsjh.gulimall.product.service.AttrGroupService;
import com.atsjh.common.utils.PageUtils;
import com.atsjh.common.utils.R;

import javax.annotation.Resource;


/**
 * 属性分组
 *
 * @author jiahuansong
 * @email jiahuansong@qq.com
 * @date 2021-06-08 14:18:44
 */
@RestController
@RequestMapping("product/attrgroup")
public class AttrGroupController {
    @Autowired
    private AttrGroupService attrGroupService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    AttrService attrService;

    @Resource
    AttrAttrgroupRelationDao attrAttrgroupRelationDao;

    @Autowired
    AttrAttrgroupRelationService attrAttrgroupRelationService;

    /**
     * /api/product/attrgroup/1/attr/relation
     */
    @RequestMapping("/{attrGroupId}/attr/relation")
    public R getRelatedAttr(@PathVariable("attrGroupId") Long attrGroupId){
        List<AttrEntity> attrs= attrService.getRelatedAttr(attrGroupId);
        return R.ok().put("data", attrs);
    }


    /**
     * /attr/relation/delete
     */
    @RequestMapping("/attr/relation/delete")
    public R deleteRelation(@RequestBody AttrAttrgroupRelationEntity[] array){
        if(array != null && array.length != 0)
        attrAttrgroupRelationDao.deleteRelation(array);
        return R.ok();
    }

    /**
     * /attrgroup/1/noattr/relation
     */
    @RequestMapping("/{attrGroupId}/noattr/relation")
    public R geNoAttrRelation(@RequestParam Map<String, Object> params, @PathVariable("attrGroupId") Long attrGroupId){
        PageUtils page = attrService.geNoAttrRelation(params, attrGroupId);
        return R.ok().put("page", page);
    }

    /**
     * /product/attrgroup/attr/relation
     */
    @RequestMapping("/attr/relation")
    public R saveAttrRelation(@RequestBody List<AttrAttrgroupRelationEntity> list){
        attrAttrgroupRelationService.saveBatch(list);
        return R.ok();
    }

    /**
     * 获取分类下所有分组&关联属性
     * /product/attrgroup/{catelogId}/withattr
     */
    @RequestMapping("/{catelogId}/withattr")
    public R getAttrGroupsWithAttrs(@PathVariable("catelogId") Long catelogId){
        List<AttrGroupWithAttrsVo> data = attrGroupService.getAttrGroupsWithAttrs(catelogId);
        return R.ok().put("data", data);
    }


    /**
     * 列表
     */
    @RequestMapping("/list/{catgoryId}")
    //@RequiresPermissions("product:attrgroup:list")
    public R list(@RequestParam Map<String, Object> params, @PathVariable Long catgoryId){
//        PageUtils page = attrGroupService.queryPage(params);
        PageUtils page = attrGroupService.queryPage(params, catgoryId);
        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrGroupId}")
    //@RequiresPermissions("product:attrgroup:info")
    public R info(@PathVariable("attrGroupId") Long attrGroupId){
		AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);

        Long[] path = categoryService.getLogPath(attrGroupId);
        attrGroup.setCatelogPath(path);
        return R.ok().put("attrGroup", attrGroup);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:attrgroup:save")
    public R save(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.save(attrGroup);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:attrgroup:update")
    public R update(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.updateById(attrGroup);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:attrgroup:delete")
    public R delete(@RequestBody Long[] attrGroupIds){
		attrGroupService.removeByIds(Arrays.asList(attrGroupIds));

        return R.ok();
    }

}
