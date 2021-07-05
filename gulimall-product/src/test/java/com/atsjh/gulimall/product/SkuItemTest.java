package com.atsjh.gulimall.product;

import com.atsjh.gulimall.product.service.SkuInfoService;
import com.atsjh.gulimall.product.vo.SkuItemSaleAttrVo;
import com.atsjh.gulimall.product.vo.SkuItemVo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sound.midi.Soundbank;
import java.util.concurrent.ExecutionException;

/**
 * @author: sjh
 * @date: 2021/7/4 下午3:12
 * @description:
 */
@Slf4j
@SpringBootTest
public class SkuItemTest {
    @Autowired
    SkuInfoService skuInfoService;

    @Test
    public void test() throws ExecutionException, InterruptedException {
        SkuItemVo item = skuInfoService.item(29L);
        for(SkuItemSaleAttrVo attr: item.getSaleAttr()){
            System.out.println(attr);
        }
    }
}
