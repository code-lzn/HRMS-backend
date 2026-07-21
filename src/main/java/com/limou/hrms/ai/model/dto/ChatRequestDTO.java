package com.limou.hrms.ai.model.dto;

import lombok.Data;

/**
 * AI 对话请求 DTO
 */
@Data
public class ChatRequestDTO {

    /**
     * 会话ID（UUID），用于关联多轮对话
     */
    private String sessionId;

    /**
     * 用户输入的消息
     */
    private String message;

    /**
     * 用户ID（由后端从 Session 中获取，前端可不传）
     */
    private Long userId;
}
