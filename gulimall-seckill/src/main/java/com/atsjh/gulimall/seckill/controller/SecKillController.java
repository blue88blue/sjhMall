package com.atsjh.gulimall.seckill.controller;

import com.atsjh.common.utils.R;
import com.atsjh.gulimall.seckill.service.SecKillService;
import com.atsjh.gulimall.seckill.to.SeckillSkuRedisTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * @author: sjh
 * @date: 2021/7/16 下午6:48
 * @description:
 */
@Controller
public class SecKillController {
    @Autowired
    SecKillService secKillService;
    /**
     * 当前时间可以参与秒杀的商品信息
     * @return
     */
    @GetMapping(value = "/getCurrentSeckillSkus")
    @ResponseBody
    public R getCurrentSeckillSkus() {
        //获取到当前可以参加秒杀商品的信息
        List<SeckillSkuRedisTo> vos = secKillService.getCurrentSeckillSkus();

        return R.ok().setData(vos);
    }

}
