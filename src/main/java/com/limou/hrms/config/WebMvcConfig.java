package com.limou.hrms.config;

import com.limou.hrms.interceptor.LoginInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置 — 注册登录拦截器。
 * 审批中心当前用户信息由 {@code AuthCheck} AOP 注入 DataScopeContext，不再需要单独拦截器。
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
