package com.limou.hrms.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * AI 模块配置
 */
@Configuration
public class AIConfig {

    /**
     * AI 对话专用线程池（SSE 异步处理）
     */
    @Bean("aiChatExecutor")
    public Executor aiChatExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-chat-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    /**
     * LLM 配置属性
     */
    @Bean
    @ConfigurationProperties(prefix = "ai.llm")
    public LLMProperties llmProperties() {
        return new LLMProperties();
    }

    @Data
    public static class LLMProperties {
        /**
         * LLM API 地址
         */
        private String apiUrl = "https://api.deepseek.com/v1/chat/completions";

        /**
         * API Key
         */
        private String apiKey = "";

        /**
         * 模型名称
         */
        private String model = "deepseek-chat";

        /**
         * 请求超时（毫秒）
         */
        private int timeout = 60000;

        /**
         * 最大 Token 数
         */
        private int maxTokens = 2048;

        /**
         * 温度参数 0-2
         */
        private double temperature = 0.7;

        /**
         * 是否启用 LLM 意图识别（false 时仅使用规则匹配）
         */
        private boolean intentLlmEnabled = true;
    }
}
