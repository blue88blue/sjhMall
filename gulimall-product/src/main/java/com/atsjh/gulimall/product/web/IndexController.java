package com.atsjh.gulimall.product.web;

import com.atsjh.gulimall.product.entity.CategoryEntity;
import com.atsjh.gulimall.product.service.CategoryService;
import com.atsjh.gulimall.product.vo.Catelog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * @author: sjh
 * @date: 2021/6/27 下午2:24
 * @description:
 */
@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    StringRedisTemplate redisTemplate;

    @GetMapping({"/", "index.html"})
    public String indexPage(Model model){
        List<CategoryEntity> list= categoryService.getLevel1Categorys();
        model.addAttribute("categories", list);
        return "index";
    }

    @GetMapping(value = "/index/catalog.json")
    @ResponseBody
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        return categoryService.getCatalogJson();
    }


    @GetMapping("hello")
    @ResponseBody
    public String hello(){
        RLock my_lock = redissonClient.getLock("my_lock");
        my_lock.lock();
        try {
            System.out.println("获取到锁， 执行业务:"+Thread.currentThread().getName());
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            my_lock.unlock();
        }
        return "hello啊";
    }

    @GetMapping("/write")
    @ResponseBody
    public String write(){
        RReadWriteLock rw_lock = redissonClient.getReadWriteLock("rw_lock");
        RLock rLock = rw_lock.writeLock();
        rLock.lock();
        try {
            System.out.println("获取到锁， 正在写入...:");
            redisTemplate.opsForValue().set("text", Thread.currentThread().getName());
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            rLock.unlock();
        }
        return Thread.currentThread().getName();
    }

    @GetMapping("/read")
    @ResponseBody
    public String read(){
        RReadWriteLock rw_lock = redissonClient.getReadWriteLock("rw_lock");
        RLock rLock = rw_lock.readLock();
        rLock.lock();
        String text = "";
        try {
            System.out.println("获取到读锁");
            text = redisTemplate.opsForValue().get("text");
        }finally {
            rLock.unlock();
        }
        return text;
    }
}
