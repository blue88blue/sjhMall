package com.atsjh.gulimall.member;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;


/**
 * 想要远程调用别的服务
 *    1.引入open-feign
 *    2.编写一个接口， 告诉springcloud这个借口需要调用远程服务
 *          声明接口的每一个方法都是调用远程服务的那个请求
 *    3.开启远程调用功能
 */
@EnableFeignClients("com.atsjh.gulimall.member.feign") //扫描接口方法注解
@EnableDiscoveryClient
@MapperScan("com.atsjh.gulimall.member.dao")
@SpringBootApplication
public class GulimallMemberApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallMemberApplication.class, args);
    }

}
