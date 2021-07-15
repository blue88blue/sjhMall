package com.atsjh.gulimall.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author: sjh
 * @date: 2021/7/4 下午8:01
 * @description:
 */
@Data
@Component
@ConfigurationProperties("gulimall-thread")
public class ThreadPoolConfigProperties {
    private Integer corePoolSize;
    private Integer maximumPoolSize;
    private Long keepAliveTime;
}
