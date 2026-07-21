package com.limou.hrms.ai.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.limou.hrms.ai.config.AIConfig;
import com.limou.hrms.ai.mapper.AiIntentRouteMapper;
import com.limou.hrms.ai.model.entity.AiIntentRoute;
import com.limou.hrms.constant.AIConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 意图识别服务
 * 双路策略：规则匹配（快）+ LLM 语义识别（兜底）
 */
@Service
@Slf4j
public class IntentRecognitionService {

    @Resource
    private AiIntentRouteMapper intentRouteMapper;

    @Resource
    private LLMService llmService;

    @Resource
    private AIConfig.LLMProperties llmProperties;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_KEY_ROUTES = "ai:intent_routes";
    private static final String REDIS_KEY_INTENT_PROMPT = "ai:intent_llm_prompt";

    /**
     * 意图识别结果
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class IntentResult {
        /** 是否命中 */
        private boolean matched;
        /** 意图名称 */
        private String intentName;
        /** 置信度 0-1 */
        private double confidence;
        /** 路由路径 */
        private String routePath;
        /** 路由名称 */
        private String routeLabel;
        /** 响应类型：route_only/answer_route/both */
        private String responseType;
        /** 匹配来源：rule/llm */
        private String source;
    }

    /**
     * 识别用户意图
     *
     * @param message 用户消息
     * @return 意图识别结果
     */
    public IntentResult recognize(String message) {
        if (StrUtil.isBlank(message)) {
            return IntentResult.builder().matched(false).build();
        }

        // 第一路：规则关键词匹配（毫秒级）
        IntentResult ruleResult = matchByRules(message);
        if (ruleResult.isMatched() && ruleResult.getConfidence() >= 0.8) {
            log.info("[意图识别] 规则命中: intent={}, keywords matched", ruleResult.getIntentName());
            return ruleResult;
        }

        // 第二路：LLM 语义识别（仅在启用时）
        if (llmProperties.isIntentLlmEnabled() && StrUtil.isNotBlank(llmProperties.getApiKey())) {
            log.info("[意图识别] 规则未命中，走 LLM 语义识别");
            IntentResult llmResult = matchByLLM(message);
            if (llmResult.isMatched()) {
                return llmResult;
            }
        }

        // 兜底：一般问答
        return IntentResult.builder()
                .matched(true)
                .intentName("general_qa")
                .confidence(0.5)
                .responseType("answer_route")
                .source("fallback")
                .build();
    }

    /**
     * 规则关键词匹配
     */
    private IntentResult matchByRules(String message) {
        List<AiIntentRoute> routes = getCachedRoutes();
        if (routes.isEmpty()) {
            return IntentResult.builder().matched(false).build();
        }

        AiIntentRoute bestMatch = null;
        int bestScore = 0;

        for (AiIntentRoute route : routes) {
            int score = countKeywordMatch(message, route.getKeywords());
            if (score > bestScore) {
                bestScore = score;
                bestMatch = route;
            }
        }

        if (bestMatch != null && bestScore > 0) {
            double confidence = Math.min(bestScore * 0.25, 1.0); // 每个关键词命中+0.25
            return IntentResult.builder()
                    .matched(true)
                    .intentName(bestMatch.getIntentName())
                    .confidence(confidence)
                    .routePath(bestMatch.getRoutePath())
                    .routeLabel(bestMatch.getRouteLabel())
                    .responseType(bestMatch.getResponseType())
                    .source("rule")
                    .build();
        }

        return IntentResult.builder().matched(false).build();
    }

    /**
     * LLM 语义意图识别
     */
    private IntentResult matchByLLM(String message) {
        String prompt = getIntentLLMPrompt();
        String llmResponse = llmService.chatSync(prompt, "用户输入：" + message);

        if (StrUtil.isBlank(llmResponse)) {
            return IntentResult.builder().matched(false).build();
        }

        // 解析 LLM 返回的意图名称
        String intentName = llmResponse.trim().toLowerCase();
        log.info("[意图识别] LLM 返回意图: {}", intentName);

        // 查找对应的路由配置
        List<AiIntentRoute> routes = getCachedRoutes();
        Optional<AiIntentRoute> matchedRoute = routes.stream()
                .filter(r -> r.getIntentName().equalsIgnoreCase(intentName))
                .findFirst();

        if (matchedRoute.isPresent()) {
            AiIntentRoute route = matchedRoute.get();
            return IntentResult.builder()
                    .matched(true)
                    .intentName(route.getIntentName())
                    .confidence(0.7)
                    .routePath(route.getRoutePath())
                    .routeLabel(route.getRouteLabel())
                    .responseType(route.getResponseType())
                    .source("llm")
                    .build();
        }

        return IntentResult.builder()
                .matched(true)
                .intentName("general_qa")
                .confidence(0.6)
                .responseType("answer_route")
                .source("llm")
                .build();
    }

    /**
     * 获取缓存的路由规则
     */
    @SuppressWarnings("unchecked")
    private List<AiIntentRoute> getCachedRoutes() {
        // 先从 Redis 取
        Object cached = redisTemplate.opsForValue().get(REDIS_KEY_ROUTES);
        if (cached instanceof List) {
            return (List<AiIntentRoute>) cached;
        }

        // 缓存未命中，从 DB 加载
        LambdaQueryWrapper<AiIntentRoute> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiIntentRoute::getStatus, 1)
                .orderByDesc(AiIntentRoute::getPriority);
        List<AiIntentRoute> routes = intentRouteMapper.selectList(wrapper);

        if (!routes.isEmpty()) {
            redisTemplate.opsForValue().set(REDIS_KEY_ROUTES, routes, 30, TimeUnit.MINUTES);
        }
        return routes;
    }

    /**
     * 获取意图识别的 LLM Prompt
     */
    private String getIntentLLMPrompt() {
        // 先从 Redis 取
        Object cached = redisTemplate.opsForValue().get(REDIS_KEY_INTENT_PROMPT);
        if (cached instanceof String) {
            return (String) cached;
        }

        // 动态构建 Prompt
        List<AiIntentRoute> routes = getCachedRoutes();
        StringBuilder intentList = new StringBuilder();
        for (AiIntentRoute route : routes) {
            intentList.append("- ").append(route.getIntentName()).append("\n");
        }

        String prompt = AIConstant.INTENT_CLASSIFY_PROMPT_TEMPLATE
                .replace("{INTENT_LIST}", intentList.toString());

        redisTemplate.opsForValue().set(REDIS_KEY_INTENT_PROMPT, prompt, 10, TimeUnit.MINUTES);
        return prompt;
    }

    /**
     * 统计用户消息中命中的关键词数量
     */
    private int countKeywordMatch(String message, String keywords) {
        if (StrUtil.isBlank(keywords)) {
            return 0;
        }
        int count = 0;
        String msg = message.toLowerCase();
        for (String kw : keywords.split(",")) {
            if (StrUtil.isNotBlank(kw) && msg.contains(kw.trim().toLowerCase())) {
                count++;
            }
        }
        return count;
    }

    /**
     * 清除路由缓存（管理员修改意图路由后调用）
     */
    public void clearRouteCache() {
        redisTemplate.delete(REDIS_KEY_ROUTES);
        redisTemplate.delete(REDIS_KEY_INTENT_PROMPT);
        log.info("[意图识别] 路由缓存已清除");
    }
}
