package com.limou.hrms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * Web MVC 配置 — 注册拦截器
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private EmployeeResolveInterceptor employeeResolveInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(employeeResolveInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/health", "/user/login", "/user/register");
    }
}
