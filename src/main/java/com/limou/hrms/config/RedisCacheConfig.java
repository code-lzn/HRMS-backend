package com.limou.hrms.config;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;

/**
 * Redis 缓存配置
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    /** 待办红点数量缓存 TTL：30 秒 */
    private static final Duration PENDING_COUNT_TTL = Duration.ofSeconds(30);
    /** 委托路由缓存 TTL：60 秒（委托期间敏感，需要较频繁刷新） */
    private static final Duration DELEGATE_ROUTING_TTL = Duration.ofSeconds(60);
    /** 部门负责人缓存 TTL：30 分钟 */
    private static final Duration DEPT_MANAGER_TTL = Duration.ofMinutes(30);
    /** HR负责人列表缓存 TTL：1 小时 */
    private static final Duration HR_APPROVER_TTL = Duration.ofHours(1);
    /** 审批节点配置缓存 TTL：1 小时 */
    private static final Duration APPROVAL_NODE_CONFIG_TTL = Duration.ofHours(1);

    @Bean
    public RedisCacheManagerBuilderCustomizer cacheTtlCustomizer() {
        return builder -> builder
                .withCacheConfiguration("pendingCount",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(PENDING_COUNT_TTL))
                .withCacheConfiguration("delegateRouting",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(DELEGATE_ROUTING_TTL))
                .withCacheConfiguration("deptManager",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(DEPT_MANAGER_TTL))
                .withCacheConfiguration("hrApprover",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(HR_APPROVER_TTL))
                .withCacheConfiguration("approvalNodeConfig",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(APPROVAL_NODE_CONFIG_TTL));
    }
}
