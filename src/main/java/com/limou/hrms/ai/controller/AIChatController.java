package com.limou.hrms.ai.controller;

import cn.hutool.core.util.StrUtil;
import com.limou.hrms.ai.model.dto.ChatFeedbackDTO;
import com.limou.hrms.ai.model.dto.ChatRequestDTO;
import com.limou.hrms.ai.model.vo.ChatMessageVO;
import com.limou.hrms.ai.model.vo.ChatSessionVO;
import com.limou.hrms.ai.service.AIChatService;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.constant.AIConstant;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.model.entity.User;
import com.limou.hrms.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * AI 智能助理接口
 * 提供 SSE 流式对话、历史记录查询、会话管理等功能
 */
@RestController
@RequestMapping("/ai")
@Slf4j
public class AIChatController {

    @Resource
    private AIChatService aiChatService;

    @Resource
    private UserService userService;

    /**
     * SSE 流式对话
     *
     * @param requestDTO 对话请求（sessionId 为空时自动创建）
     * @param request    HTTP 请求（获取登录用户）
     * @return SseEmitter 流式响应
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequestDTO requestDTO,
                                  HttpServletRequest request) {
        // 参数校验
        if (requestDTO == null || StrUtil.isBlank(requestDTO.getMessage())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息不能为空");
        }

        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 会话ID为空时自动生成
        String sessionId = requestDTO.getSessionId();
        if (StrUtil.isBlank(sessionId)) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
        }

        SseEmitter emitter = new SseEmitter(AIConstant.SSE_TIMEOUT_MS);

        String finalSessionId = sessionId;
        CompletableFuture.runAsync(() -> {
            try {
                aiChatService.handleChat(loginUser.getId(), finalSessionId,
                        requestDTO.getMessage(), emitter);
            } catch (Exception e) {
                log.error("[AI对话] 处理异常", e);
                try {
                    emitter.send(SseEmitter.event()
                            .data("{\"type\":\"error\",\"content\":\"系统异常，请稍后重试。\"}"));
                    emitter.complete();
                } catch (Exception ex) {
                    log.debug("[SSE] 异常通知发送失败");
                }
            }
        });

        // 注册超时和完成回调
        emitter.onTimeout(() -> log.debug("[SSE] 连接超时: sessionId={}", finalSessionId));
        emitter.onCompletion(() -> log.debug("[SSE] 连接完成: sessionId={}", finalSessionId));
        emitter.onError(throwable -> log.error("[SSE] 连接错误: sessionId={}", finalSessionId, throwable));

        return emitter;
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/sessions")
    public BaseResponse<List<ChatSessionVO>> getSessionList(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        List<ChatSessionVO> sessions = aiChatService.getSessionList(loginUser.getId());
        return ResultUtils.success(sessions);
    }

    /**
     * 获取指定会话的历史消息
     */
    @GetMapping("/history/{sessionId}")
    public BaseResponse<List<ChatMessageVO>> getChatHistory(@PathVariable String sessionId,
                                                             HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        List<ChatMessageVO> messages = aiChatService.getChatHistory(loginUser.getId(), sessionId);
        return ResultUtils.success(messages);
    }

    /**
     * 提交对话反馈（点赞/点踩）
     */
    @PostMapping("/feedback")
    public BaseResponse<Boolean> submitFeedback(@RequestBody ChatFeedbackDTO feedbackDTO,
                                                 HttpServletRequest request) {
        if (feedbackDTO == null || feedbackDTO.getChatId() == null || feedbackDTO.getFeedback() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        aiChatService.submitFeedback(feedbackDTO.getChatId(), feedbackDTO.getFeedback());
        return ResultUtils.success(true);
    }

    /**
     * 删除指定会话
     */
    @DeleteMapping("/session/{sessionId}")
    public BaseResponse<Boolean> deleteSession(@PathVariable String sessionId,
                                                HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        aiChatService.deleteSession(loginUser.getId(), sessionId);
        return ResultUtils.success(true);
    }

    /**
     * 新建会话（返回新会话ID）
     */
    @PostMapping("/session/new")
    public BaseResponse<String> newSession(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        return ResultUtils.success(sessionId);
    }
}
