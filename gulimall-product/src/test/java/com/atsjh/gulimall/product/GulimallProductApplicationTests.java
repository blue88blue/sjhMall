package com.atsjh.gulimall.product;


import com.atsjh.gulimall.product.entity.BrandEntity;
import com.atsjh.gulimall.product.service.BrandService;
import com.atsjh.gulimall.product.service.CategoryService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.PriorityQueue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

@Slf4j
@SpringBootTest
class GulimallProductApplicationTests {

    @Autowired
    BrandService brandService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Test
    void contextLoads() {

        BrandEntity brandEntity = new BrandEntity();
        brandEntity.setBrandId(1L);
        brandEntity.setDescript("华为中国");

        brandService.updateById(brandEntity);
    }

    @Test
    void test() {
        List<BrandEntity> list = brandService.list(new QueryWrapper<BrandEntity>().eq("brand_id", 1L));
        list.forEach((item) -> System.out.println(item));

    }

    @Test
    void findPathTest(){
        Long[] logPath = categoryService.getLogPath((long) 252);
        log.info("完整路径：{}", Arrays.asList(logPath));
    }

    @Test
    public void RedisTest(){
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        ops.set("hello", "world!!");

        System.out.println(ops.get("hello"));
    }


}
