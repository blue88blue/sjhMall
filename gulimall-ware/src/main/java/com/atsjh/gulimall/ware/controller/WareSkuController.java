package com.atsjh.gulimall.ware.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import com.atsjh.gulimall.ware.vo.SkuHasStochVo;
import com.atsjh.gulimall.ware.vo.WareSkuLockVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.atsjh.gulimall.ware.entity.WareSkuEntity;
import com.atsjh.gulimall.ware.service.WareSkuService;
import com.atsjh.common.utils.PageUtils;
import com.atsjh.common.utils.R;

import javax.sound.midi.Soundbank;


/**
 * 商品库存
 *
 * @author jiahuansong
 * @email jiahuansong@qq.com
 * @date 2021-06-08 15:08:25
 */
@RestController
@RequestMapping("ware/waresku")
public class WareSkuController {
    @Autowired
    private WareSkuService wareSkuService;


    @PostMapping("lock/order")
    public R lockOder(@RequestBody WareSkuLockVo lockVo){
        boolean res = wareSkuService.orderLockStock(lockVo);
        if(res)
            return R.ok();
        else
            return R.error();
    }



    @RequestMapping("hasStock")
    public R hasStock(@RequestBody List<Long> skuIds){
        List<SkuHasStochVo> data = wareSkuService.hasStock(skuIds);
        return R.ok().setData(data);
    }


    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("ware:waresku:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = wareSkuService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("ware:waresku:info")
    public R info(@PathVariable("id") Long id){
		WareSkuEntity wareSku = wareSkuService.getById(id);

        return R.ok().put("wareSku", wareSku);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("ware:waresku:save")
    public R save(@RequestBody WareSkuEntity wareSku){
		wareSkuService.save(wareSku);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("ware:waresku:update")
    public R update(@RequestBody WareSkuEntity wareSku){
		wareSkuService.updateById(wareSku);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("ware:waresku:delete")
    public R delete(@RequestBody Long[] ids){
		wareSkuService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
