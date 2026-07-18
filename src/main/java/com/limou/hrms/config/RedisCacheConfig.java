package com.limou.hrms.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

/**
 * Redis 缓存配置
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    /** 待办红点数量缓存 TTL：30 秒 */
    private static final Duration PENDING_COUNT_TTL = Duration.ofSeconds(30);
    /** 委托路由缓存 TTL：60 秒 */
    private static final Duration DELEGATE_ROUTING_TTL = Duration.ofSeconds(60);
    /** 部门负责人缓存 TTL：30 分钟 */
    private static final Duration DEPT_MANAGER_TTL = Duration.ofMinutes(30);
    /** HR负责人列表缓存 TTL：1 小时 */
    private static final Duration HR_APPROVER_TTL = Duration.ofHours(1);
    /** 审批节点配置缓存 TTL：1 小时 */
    private static final Duration APPROVAL_NODE_CONFIG_TTL = Duration.ofHours(1);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig();
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("pendingCount", defaultConfig.entryTtl(PENDING_COUNT_TTL))
                .withCacheConfiguration("delegateRouting", defaultConfig.entryTtl(DELEGATE_ROUTING_TTL))
                .withCacheConfiguration("deptManager", defaultConfig.entryTtl(DEPT_MANAGER_TTL))
                .withCacheConfiguration("hrApprover", defaultConfig.entryTtl(HR_APPROVER_TTL))
                .withCacheConfiguration("approvalNodeConfig", defaultConfig.entryTtl(APPROVAL_NODE_CONFIG_TTL))
                .build();
    }
}
