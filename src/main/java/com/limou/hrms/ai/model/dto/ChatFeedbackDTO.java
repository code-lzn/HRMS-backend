package com.limou.hrms.ai.model.dto;

import lombok.Data;

/**
 * 对话反馈 DTO
 */
@Data
public class ChatFeedbackDTO {

    /**
     * 对话记录ID
     */
    private Long chatId;

    /**
     * 反馈：1=点赞 0=点踩
     */
    private Integer feedback;
}
