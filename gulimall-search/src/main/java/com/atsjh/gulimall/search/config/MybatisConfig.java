package com.atsjh.gulimall.search.config;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author: sjh
 * @date: 2021/6/26 下午9:19
 * @description:
 */
@Configuration
public class MybatisConfig {
    @Bean
    MybatisPlusProperties mybatisPlusProperties(){
        return new MybatisPlusProperties();
    }
}
