package com.limou.hrms.config;

import com.limou.hrms.interceptor.PermissionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * 注册权限拦截器，对受保护 URL 强制校验权限码
 */
@Configuration
public class PermissionWebConfig implements WebMvcConfigurer {

    @Resource
    private PermissionInterceptor permissionInterceptor;

//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        registry.addInterceptor(permissionInterceptor)
//                .addPathPatterns("/**")
//                .excludePathPatterns(
//                        // 登录注册
//                        "/api/user/login",
//                        "/api/user/register",
//                        "/api/user/logout",
//                        // 权限查询（自身）
//                        "/api/permission/**",
//                        // 公开的角色列表（登录页选角色下拉框用）
//                        "/api/role/list/enabled",
//                        // Swagger / Knife4j 文档
//                        "/v2/api-docs/**",
//                        "/swagger-resources/**",
//                        "/swagger-ui.html",
//                        "/doc.html",
//                        "/webjars/**",
//                        // 静态资源
//                        "/error",
//                        "/favicon.ico"
//                );
//    }
}
