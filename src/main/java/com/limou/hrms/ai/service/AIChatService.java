package com.limou.hrms.ai.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.limou.hrms.ai.mapper.AiChatHistoryMapper;
import com.limou.hrms.ai.model.entity.AiChatHistory;
import com.limou.hrms.ai.model.entity.AiKnowledgeDoc;
import com.limou.hrms.ai.model.vo.ChatMessageVO;
import com.limou.hrms.ai.model.vo.ChatSessionVO;
import com.limou.hrms.ai.service.IntentRecognitionService.IntentResult;
import com.limou.hrms.constant.AIConstant;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.Role;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.service.EmployeeService;
import com.limou.hrms.service.RoleService;
import com.limou.hrms.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 智能助理核心服务
 * 编排整个对话流程：意图识别 → 知识检索 → LLM 生成 → 路由推送
 */
@Service
@Slf4j
public class AIChatService {

    @Resource
    private IntentRecognitionService intentRecognitionService;

    @Resource
    private KnowledgeSearchService knowledgeSearchService;

    @Resource
    private LLMService llmService;

    @Resource
    private AiChatHistoryMapper chatHistoryMapper;

    @Resource
    private EmployeeService employeeService;

    @Resource
    private RoleService roleService;

    @Resource
    private UserService userService;

    /**
     * 处理用户对话并流式返回（SSE）
     * 在独立线程中执行，避免阻塞主线程
     *
     * @param userId     当前登录用户ID
     * @param sessionId  会话ID
     * @param message    用户消息
     * @param emitter    SSE 推送器
     */
    public void handleChat(Long userId, String sessionId, String message, SseEmitter emitter) {
        // ========== 同步预处理阶段 ==========

        // 1. 保存用户消息
        saveChatHistory(userId, sessionId, "user", message, null, null, null);

        // 2. 发送思考状态
        sendSSE(emitter, ChatMessageVO.builder().type("thinking").content("正在分析您的问题...").build());

        // 3. 意图识别
        IntentResult intent = intentRecognitionService.recognize(message);
        log.info("[AI对话] userId={}, intent={}, confidence={}, source={}",
                userId, intent.getIntentName(), intent.getConfidence(), intent.getSource());

        // 4. 发送意图信息
        sendSSE(emitter, ChatMessageVO.builder()
                .type("intent")
                .intent(intent.getIntentName())
                .confidence(intent.getConfidence())
                .build());

        // 5. route_only 场景：直接推送路由，不调用 LLM
        if ("route_only".equals(intent.getResponseType()) && StrUtil.isNotBlank(intent.getRoutePath())) {
            String text = "已为您定位到对应功能，请点击下方按钮跳转～";
            sendSSE(emitter, ChatMessageVO.builder().type("content").content(text).build());
            sendSSE(emitter, buildRouteEvent(intent));
            sendDoneAndComplete(emitter);

            saveChatHistory(userId, sessionId, "assistant", text,
                    intent.getIntentName(), intent.getRoutePath(), intent.getRouteLabel());
            return;
        }

        // 6. 检索知识库
        List<AiKnowledgeDoc> knowledgeDocs = knowledgeSearchService.search(message, AIConstant.KNOWLEDGE_TOP_K);
        String knowledgeContext = buildKnowledgeContext(knowledgeDocs);

        // 7. 获取用户信息并构建 System Prompt
        User user = userService.getById(userId);
        Employee employee = employeeService.getByUserId(userId);
        String userName = employee != null ? employee.getEmployeeName() : "员工";
        Role role = user != null && user.getRoleId() != null
                ? roleService.getById(user.getRoleId()) : null;
        String userRole = role != null ? role.getRoleName() : "普通员工";
        String systemPrompt = buildSystemPrompt(knowledgeContext, userId, userName, userRole);

        // 8. 获取历史对话
        List<Map<String, String>> history = getRecentHistory(userId, sessionId);

        // 9. 构建增强用户消息
        String enhancedMessage = buildEnhancedMessage(message, intent);

        // ========== 流式响应生成阶段 ==========

        String llmResponse;

        if (llmService.isConfigured()) {
            // LLM 已配置：使用 LLM 流式生成
            sendSSE(emitter, ChatMessageVO.builder().type("thinking").content("正在整理回答...").build());

            llmResponse = llmService.chatStream(systemPrompt, enhancedMessage, history, chunk -> {
                sendSSE(emitter, ChatMessageVO.builder().type("content").content(chunk).build());
            });
        } else {
            // LLM 未配置：降级为知识库检索 + 模板拼接
            log.info("[AI对话] LLM 未配置，使用知识库降级回复");
            llmResponse = buildFallbackResponse(knowledgeDocs, intent, message);
            sendSSE(emitter, ChatMessageVO.builder().type("content").content(llmResponse).build());
        }

        // ========== 后处理阶段 ==========

        // 10. 提取并推送路由（如果 LLM 回复中包含 [ROUTE:...] 标记）
        RouteInfo parsedRoute = parseRouteTag(llmResponse);
        if (parsedRoute != null) {
            sendSSE(emitter, buildRouteEvent(parsedRoute));
        } else if (isAnswerRouteOrBoth(intent)) {
            // LLM 未生成路由标签，用意图识别的结果补充
            sendSSE(emitter, buildRouteEvent(intent));
        }

        // 11. 清理 [ROUTE:...] 标记，保存纯净的回复文本
        String cleanResponse = cleanRouteTag(llmResponse);

        // 12. 保存助手回复
        String finalRoutePath = parsedRoute != null ? parsedRoute.path : intent.getRoutePath();
        String finalRouteLabel = parsedRoute != null ? parsedRoute.label : intent.getRouteLabel();
        saveChatHistory(userId, sessionId, "assistant", cleanResponse,
                intent.getIntentName(), finalRoutePath, finalRouteLabel);

        // 13. 发送完成事件
        sendDoneAndComplete(emitter);
    }

    /**
     * 获取用户的会话列表
     */
    public List<ChatSessionVO> getSessionList(Long userId) {
        LambdaQueryWrapper<AiChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(AiChatHistory::getSessionId, AiChatHistory::getUserId,
                        AiChatHistory::getRole, AiChatHistory::getContent,
                        AiChatHistory::getCreateTime)
                .eq(AiChatHistory::getUserId, userId)
                .orderByDesc(AiChatHistory::getCreateTime);
        List<AiChatHistory> records = chatHistoryMapper.selectList(wrapper);

        // 按 sessionId 分组（保持插入顺序）
        Map<String, List<AiChatHistory>> sessionMap = records.stream()
                .collect(Collectors.groupingBy(AiChatHistory::getSessionId,
                        LinkedHashMap::new, Collectors.toList()));

        List<ChatSessionVO> sessions = new ArrayList<>();
        for (Map.Entry<String, List<AiChatHistory>> entry : sessionMap.entrySet()) {
            List<AiChatHistory> msgs = entry.getValue();
            if (msgs.isEmpty()) continue;

            ChatSessionVO vo = new ChatSessionVO();
            vo.setSessionId(entry.getKey());

            // 取第一条用户消息的前20字符作为标题
            AiChatHistory firstUserMsg = msgs.stream()
                    .filter(m -> "user".equals(m.getRole()))
                    .reduce((a, b) -> b) // 取最后一条用户消息
                    .orElse(msgs.get(msgs.size() - 1));
            String title = firstUserMsg.getContent();
            if (title != null && title.length() > 20) {
                title = title.substring(0, 20) + "...";
            }
            vo.setTitle(title);
            vo.setLastMessageTime(msgs.get(0).getCreateTime());
            vo.setMessageCount(msgs.size());
            sessions.add(vo);
        }
        return sessions;
    }

    /**
     * 获取指定会话的历史消息
     */
    public List<ChatMessageVO> getChatHistory(Long userId, String sessionId) {
        LambdaQueryWrapper<AiChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatHistory::getUserId, userId)
                .eq(AiChatHistory::getSessionId, sessionId)
                .orderByAsc(AiChatHistory::getCreateTime);
        List<AiChatHistory> records = chatHistoryMapper.selectList(wrapper);

        return records.stream().map(r -> ChatMessageVO.builder()
                .type(r.getRole())
                .content(r.getContent())
                .intent(r.getIntent())
                .path(r.getRoutePath())
                .label(r.getRouteLabel())
                .messageId(r.getId())
                .build()).collect(Collectors.toList());
    }

    /**
     * 提交用户反馈
     */
    public void submitFeedback(Long chatId, Integer feedback) {
        AiChatHistory record = chatHistoryMapper.selectById(chatId);
        if (record != null && "assistant".equals(record.getRole())) {
            record.setFeedback(feedback);
            chatHistoryMapper.updateById(record);
            log.info("[AI反馈] chatId={}, feedback={}", chatId, feedback);
        }
    }

    /**
     * 删除指定会话
     */
    public void deleteSession(Long userId, String sessionId) {
        LambdaQueryWrapper<AiChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatHistory::getUserId, userId)
                .eq(AiChatHistory::getSessionId, sessionId);
        chatHistoryMapper.delete(wrapper);
        log.info("[AI对话] 会话已删除: userId={}, sessionId={}", userId, sessionId);
    }

    // ========== 私有方法 ==========

    /**
     * 路由信息（从 LLM 回复中解析）
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class RouteInfo {
        private String path;
        private String label;
    }

    /**
     * 降级回复：当 LLM 未配置时，基于知识库检索结果生成回复
     */
    private String buildFallbackResponse(List<AiKnowledgeDoc> docs, IntentResult intent, String query) {
        if (docs.isEmpty()) {
            return "您好！我是 HRMS 智能助理小智。\n\n" +
                    "抱歉，目前 AI 服务尚未配置，且未找到与您问题相关的知识库文档。\n\n" +
                    "建议您：\n" +
                    "1. 联系管理员配置 AI 服务以获得更智能的回复\n" +
                    "2. 在系统侧边栏菜单中查找相关功能\n" +
                    "3. 咨询 HR 部门获取帮助";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("您好！我是 HRMS 智能助理小智。\n\n");
        sb.append("以下是关于「").append(query).append("」的相关信息：\n\n");

        for (int i = 0; i < docs.size(); i++) {
            AiKnowledgeDoc doc = docs.get(i);
            sb.append("**").append(i + 1).append(". ").append(doc.getTitle()).append("**\n");
            sb.append(doc.getContent()).append("\n\n");
        }

        if (StrUtil.isNotBlank(intent.getRoutePath())) {
            sb.append("---\n");
            sb.append("💡 如需操作，请点击「").append(intent.getRouteLabel()).append("」功能入口。");
            sb.append("[ROUTE:").append(intent.getRoutePath()).append("|").append(intent.getRouteLabel()).append("]");
        } else {
            sb.append("---\n");
            sb.append("💡 如需更多帮助，请联系 HR 部门。");
        }

        return sb.toString();
    }

    /**
     * 构建知识库上下文
     */
    private String buildKnowledgeContext(List<AiKnowledgeDoc> docs) {
        if (docs.isEmpty()) {
            List<AiKnowledgeDoc> allDocs = knowledgeSearchService.getAllEnabled();
            if (allDocs.isEmpty()) {
                return "暂无知识库内容";
            }
            return allDocs.stream()
                    .map(d -> "【" + d.getTitle() + "】\n" + d.getContent())
                    .collect(Collectors.joining("\n\n---\n\n"));
        }
        return docs.stream()
                .map(d -> "【" + d.getTitle() + "】\n" + d.getContent())
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    /**
     * 构建 System Prompt
     */
    private String buildSystemPrompt(String knowledgeContext, Long userId, String userName, String userRole) {
        return AIConstant.SYSTEM_PROMPT_TEMPLATE
                .replace("{KNOWLEDGE_BASE}", knowledgeContext)
                .replace("{USER_ID}", String.valueOf(userId))
                .replace("{USER_NAME}", userName)
                .replace("{USER_ROLE}", userRole);
    }

    /**
     * 构建增强的用户消息（附加意图和路由信息，引导 LLM 生成路由标记）
     */
    private String buildEnhancedMessage(String message, IntentResult intent) {
        if (!intent.isMatched() || "general_qa".equals(intent.getIntentName())) {
            return message;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(message);
        sb.append("\n\n[系统提示：已识别用户意图为 ").append(intent.getIntentName()).append("。");

        if (StrUtil.isNotBlank(intent.getRoutePath())) {
            sb.append("如果你的回答涉及操作跳转，请在回复末尾严格按如下格式输出路由标记：");
            sb.append("[ROUTE:").append(intent.getRoutePath()).append("|").append(intent.getRouteLabel()).append("]");
        } else {
            sb.append("请直接回答问题。");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 获取最近的对话历史
     */
    private List<Map<String, String>> getRecentHistory(Long userId, String sessionId) {
        LambdaQueryWrapper<AiChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatHistory::getUserId, userId)
                .eq(AiChatHistory::getSessionId, sessionId)
                .orderByDesc(AiChatHistory::getCreateTime)
                .last("LIMIT " + (AIConstant.MAX_HISTORY_ROUNDS * 2));
        List<AiChatHistory> records = chatHistoryMapper.selectList(wrapper);

        // 反转为时间正序
        Collections.reverse(records);

        return records.stream()
                .map(r -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("role", r.getRole());
                    map.put("content", cleanRouteTag(r.getContent()));
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * 从 LLM 回复中解析 [ROUTE:path|label] 标记
     */
    private RouteInfo parseRouteTag(String llmResponse) {
        if (StrUtil.isBlank(llmResponse)) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\[ROUTE:([^|\\]]+)\\|([^\\]]+)\\]")
                .matcher(llmResponse);
        if (m.find()) {
            return new RouteInfo(m.group(1).trim(), m.group(2).trim());
        }
        return null;
    }

    /**
     * 清理回复中的 [ROUTE:...] 标记
     */
    private String cleanRouteTag(String text) {
        if (StrUtil.isBlank(text)) {
            return text;
        }
        return text.replaceAll("\\[ROUTE:[^\\]]+\\]", "").trim();
    }

    /**
     * 判断是否需要推送路由
     */
    private boolean isAnswerRouteOrBoth(IntentResult intent) {
        return "answer_route".equals(intent.getResponseType()) || "both".equals(intent.getResponseType());
    }

    /**
     * 构建路由 SSE 事件 VO
     */
    private ChatMessageVO buildRouteEvent(IntentResult intent) {
        return ChatMessageVO.builder()
                .type("route")
                .path(intent.getRoutePath())
                .label(intent.getRouteLabel())
                .action("push")
                .build();
    }

    private ChatMessageVO buildRouteEvent(RouteInfo route) {
        return ChatMessageVO.builder()
                .type("route")
                .path(route.path)
                .label(route.label)
                .action("push")
                .build();
    }

    /**
     * 保存对话历史
     */
    private void saveChatHistory(Long userId, String sessionId, String role, String content,
                                  String intent, String routePath, String routeLabel) {
        AiChatHistory record = new AiChatHistory();
        record.setUserId(userId);
        record.setSessionId(sessionId);
        record.setRole(role);
        record.setContent(content);
        record.setIntent(intent);
        record.setRoutePath(routePath);
        record.setRouteLabel(routeLabel);
        chatHistoryMapper.insert(record);
    }

    /**
     * 发送 SSE 事件
     */
    private void sendSSE(SseEmitter emitter, ChatMessageVO vo) {
        try {
            emitter.send(SseEmitter.event().data(toSSEData(vo)));
        } catch (IOException e) {
            log.debug("[SSE] 发送失败，客户端可能已断开");
        }
    }

    /**
     * 发送完成事件并关闭 emitter
     */
    private void sendDoneAndComplete(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().data("{\"type\":\"done\"}"));
            emitter.complete();
        } catch (Exception e) {
            log.debug("[SSE] 完成/关闭失败");
        }
    }

    /**
     * 将 ChatMessageVO 转为 SSE data JSON 字符串
     */
    private String toSSEData(ChatMessageVO vo) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"type\":\"").append(escapeJson(vo.getType())).append("\"");
        if (vo.getContent() != null) {
            sb.append(",\"content\":\"").append(escapeJson(vo.getContent())).append("\"");
        }
        if (vo.getIntent() != null) {
            sb.append(",\"intent\":\"").append(escapeJson(vo.getIntent())).append("\"");
        }
        if (vo.getConfidence() != null) {
            sb.append(",\"confidence\":").append(vo.getConfidence());
        }
        if (vo.getPath() != null) {
            sb.append(",\"path\":\"").append(escapeJson(vo.getPath())).append("\"");
        }
        if (vo.getLabel() != null) {
            sb.append(",\"label\":\"").append(escapeJson(vo.getLabel())).append("\"");
        }
        if (vo.getAction() != null) {
            sb.append(",\"action\":\"").append(escapeJson(vo.getAction())).append("\"");
        }
        if (vo.getMessageId() != null) {
            sb.append(",\"messageId\":").append(vo.getMessageId());
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
