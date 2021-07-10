package com.atsjh.gulimall.cart.config;

import com.atsjh.gulimall.cart.interceptor.CartInteceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册拦截器
 * @author: sjh
 * @date: 2021/7/8 下午4:40
 * @description:
 */
@Configuration
public class WebInteceptorConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new CartInteceptor()).addPathPatterns("/**");
    }
}
