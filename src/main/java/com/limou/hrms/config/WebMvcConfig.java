package com.limou.hrms.config;

import com.limou.hrms.interceptor.LoginInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * <p>
 * 注册登录拦截器，所有 /api/** 请求都需登录态校验，
 * 登录/注册/健康检查等公开接口排除。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注意：addPathPatterns / excludePathPatterns 的路径是相对于 context-path（/api）的
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                // 排除不需要登录的公开接口
                .excludePathPatterns(
                        "/user/login",
                        "/user/register",
                        "/health"
                );
    }
}
