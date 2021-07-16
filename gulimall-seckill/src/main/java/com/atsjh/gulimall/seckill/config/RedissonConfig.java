package com.atsjh.gulimall.seckill.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author: sjh
 * @date: 2021/6/29 上午10:33
 * @description:
 */
@Configuration
public class RedissonConfig {

    // redission通过redissonClient对象使用 // 如果是多个redis集群，可以配置
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redisson() {
        Config config = new Config();
        // 创建单例模式的配置
        config.useSingleServer().setAddress("redis://172.17.0.1:6379");
        config.useSingleServer().setPassword("sjh9323");
        return Redisson.create(config);
    }
}
