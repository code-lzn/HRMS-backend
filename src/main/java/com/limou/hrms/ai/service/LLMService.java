package com.limou.hrms.ai.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.limou.hrms.ai.config.AIConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * LLM 大模型调用服务
 * 封装对 DeepSeek/通义千问 等 OpenAI 兼容 API 的调用
 */
@Service
@Slf4j
public class LLMService {

    @Resource
    private AIConfig.LLMProperties llmProperties;

    /**
     * 同步调用 LLM（非流式），用于意图识别等轻量场景
     *
     * @param systemPrompt 系统提示词
     * @param userMessage  用户消息
     * @return LLM 返回的文本
     */
    public String chatSync(String systemPrompt, String userMessage) {
        if (StrUtil.isBlank(llmProperties.getApiKey())) {
            log.warn("[LLM] API Key 未配置，返回空");
            return "";
        }

        JSONObject body = buildRequestBody(systemPrompt, userMessage, false, null);

        try {
            HttpResponse response = HttpRequest.post(llmProperties.getApiUrl())
                    .header("Authorization", "Bearer " + llmProperties.getApiKey())
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(llmProperties.getTimeout())
                    .execute();

            if (response.getStatus() == 200) {
                JSONObject respJson = JSONUtil.parseObj(response.body());
                JSONArray choices = respJson.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    return choices.getJSONObject(0)
                            .getJSONObject("message")
                            .getStr("content", "");
                }
            } else {
                log.error("[LLM] API 调用失败，状态码: {}, 响应: {}", response.getStatus(), response.body());
            }
        } catch (Exception e) {
            log.error("[LLM] API 调用异常", e);
        }
        return "";
    }

    /**
     * 流式调用 LLM
     * 注意：此方法为同步阻塞调用（应在独立线程中执行），逐行读取 LLM 的 SSE 流
     *
     * @param systemPrompt  系统提示词
     * @param userMessage   用户消息
     * @param history       历史对话消息列表
     * @param chunkCallback 每收到一个内容块时的回调
     * @return 完整回复文本
     */
    public String chatStream(String systemPrompt, String userMessage,
                             List<Map<String, String>> history,
                             Consumer<String> chunkCallback) {
        if (StrUtil.isBlank(llmProperties.getApiKey())) {
            String fallback = "AI 服务暂未配置，请联系管理员。";
            if (chunkCallback != null) {
                chunkCallback.accept(fallback);
            }
            return fallback;
        }

        JSONObject body = buildRequestBody(systemPrompt, userMessage, true, history);

        try (HttpResponse response = HttpRequest.post(llmProperties.getApiUrl())
                .header("Authorization", "Bearer " + llmProperties.getApiKey())
                .header("Content-Type", "application/json")
                .body(body.toString())
                .timeout(llmProperties.getTimeout())
                .execute()) {

            if (response.getStatus() != 200) {
                String errorMsg = "AI 服务异常（" + response.getStatus() + "），请稍后重试。";
                log.error("[LLM] 流式调用失败，状态码: {}", response.getStatus());
                if (chunkCallback != null) {
                    chunkCallback.accept(errorMsg);
                }
                return errorMsg;
            }

            StringBuilder fullContent = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.bodyStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) {
                            break;
                        }
                        try {
                            JSONObject chunk = JSONUtil.parseObj(data);
                            JSONArray choices = chunk.getJSONArray("choices");
                            if (choices != null && !choices.isEmpty()) {
                                JSONObject delta = choices.getJSONObject(0)
                                        .getJSONObject("delta");
                                String content = delta.getStr("content");
                                if (StrUtil.isNotBlank(content)) {
                                    fullContent.append(content);
                                    if (chunkCallback != null) {
                                        chunkCallback.accept(content);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.debug("[LLM] 解析流式数据行失败: {}", data);
                        }
                    }
                }
            }

            log.info("[LLM] 流式响应完成，总长度: {}", fullContent.length());
            return fullContent.toString();

        } catch (Exception e) {
            log.error("[LLM] 流式调用异常", e);
            String errorMsg = "AI 服务异常，请稍后重试。";
            if (chunkCallback != null) {
                chunkCallback.accept(errorMsg);
            }
            return errorMsg;
        }
    }

    /**
     * 检查 LLM 服务是否已配置
     */
    public boolean isConfigured() {
        return StrUtil.isNotBlank(llmProperties.getApiKey());
    }

    // ========== 私有方法 ==========

    private JSONObject buildRequestBody(String systemPrompt, String userMessage,
                                         boolean stream, List<Map<String, String>> history) {
        JSONObject body = new JSONObject();
        body.set("model", llmProperties.getModel());
        body.set("stream", stream);
        body.set("max_tokens", llmProperties.getMaxTokens());
        body.set("temperature", llmProperties.getTemperature());

        JSONArray messages = new JSONArray();

        // 系统消息
        JSONObject sysMsg = new JSONObject();
        sysMsg.set("role", "system");
        sysMsg.set("content", systemPrompt);
        messages.add(sysMsg);

        // 注入历史消息（最多 10 轮 = 20 条）
        if (history != null && !history.isEmpty()) {
            int maxHistory = Math.min(history.size(), 20);
            int startIdx = Math.max(0, history.size() - maxHistory);
            for (int i = startIdx; i < history.size(); i++) {
                Map<String, String> msg = history.get(i);
                JSONObject histMsg = new JSONObject();
                histMsg.set("role", msg.get("role"));
                histMsg.set("content", msg.get("content"));
                messages.add(histMsg);
            }
        }

        // 用户消息
        JSONObject userMsg = new JSONObject();
        userMsg.set("role", "user");
        userMsg.set("content", userMessage);
        messages.add(userMsg);

        body.set("messages", messages);
        return body;
    }
}
