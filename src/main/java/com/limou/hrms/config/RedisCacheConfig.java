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

    @Bean
    public RedisCacheManagerBuilderCustomizer pendingCountTtlCustomizer() {
        return builder -> builder
                .withCacheConfiguration("pendingCount",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(PENDING_COUNT_TTL));
    }
}
