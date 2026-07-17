package com.limou.hrms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * Swagger / Knife4j 配置。
 * <p>
 * 开启 {@code forCodeGeneration(true)} 后，Swagger 生成的泛型定义名
 * 会使用 {@code _} 代替 {@code «»}（书名号），例如：
 * <pre>
 *   BaseResponse«List«DepartmentTreeNode»»  →  BaseResponse_List_DepartmentTreeNode_
 * </pre>
 * 从而避免前端 @umijs/openapi 的 swagger2openapi 转换报错：
 * "Could not resolve reference #/definitions/List"。
 */
@Configuration
public class SwaggerConfig {

    @Value("${knife4j.openapi.title:接口文档}")
    private String title;

    @Value("${knife4j.openapi.version:1.0}")
    private String version;

    @Bean
    public Docket docket() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(new ApiInfoBuilder()
                        .title(title)
                        .version(version)
                        .build())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.limou.hrms.controller"))
                .paths(PathSelectors.any())
                .build()
                // 关键：开启代码生成模式，用下划线替代 «» 泛型符号
                .forCodeGeneration(true);
    }
}
