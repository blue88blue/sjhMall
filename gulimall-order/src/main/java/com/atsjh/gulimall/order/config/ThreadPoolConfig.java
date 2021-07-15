package com.atsjh.gulimall.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author: sjh
 * @date: 2021/7/4 下午7:56
 * @description:
 */
@Configuration
public class ThreadPoolConfig {

    @Bean
    ThreadPoolExecutor threadPoolExecutor(ThreadPoolConfigProperties properties){
        return new ThreadPoolExecutor(properties.getCorePoolSize(), properties.getMaximumPoolSize(),
                properties.getKeepAliveTime(), TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
    }
}
