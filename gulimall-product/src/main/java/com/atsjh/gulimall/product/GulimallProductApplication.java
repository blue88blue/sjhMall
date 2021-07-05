package com.atsjh.gulimall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 整合mybatis-plus
 * 1.导入依赖
 *         <dependency>
 *             <groupId>com.baomidou</groupId>
 *             <artifactId>mybatis-plus-boot-starter</artifactId>
 *             <version>3.4.3</version>
 *         </dependency>
 * 2.配置
 *      1）配置数据源
 *          导入数据库驱动
 *          在application.yml中配置数据源相关信息
 *      2）配置mybatis-plusl
 *          使用@MapperScan
 *          告诉mybatis-plus， sql映射文件位置
 *
 */
@EnableCaching
@EnableFeignClients("com.atsjh.gulimall.product.feign")
@EnableDiscoveryClient
@MapperScan("com.atsjh.gulimall.product.dao")
@SpringBootApplication
public class GulimallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallProductApplication.class, args);
    }

}
