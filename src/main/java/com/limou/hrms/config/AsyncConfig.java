package com.limou.hrms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 异步任务配置（薪资核算异步执行）
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
