package com.atsjh.gulimall.seckill.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


/**
 * 开启异步和定时任务
 * @author: sjh
 * @date: 2021/7/16 下午1:35
 * @description:
 */
@EnableAsync
@EnableScheduling
@Configuration
public class ScheduledConfig {
}
