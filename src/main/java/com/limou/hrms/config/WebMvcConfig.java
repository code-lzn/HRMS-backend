package com.limou.hrms.config;

import com.limou.hrms.interceptor.LoginInterceptor;
import lombok.RequiredArgsConstructor;
import com.limou.hrms.interceptor.EmployeeResolveInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 *
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private EmployeeResolveInterceptor employeeResolveInterceptor;

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
        registry.addInterceptor(employeeResolveInterceptor)
                .addPathPatterns("/**/approvals/**");
    }
}
