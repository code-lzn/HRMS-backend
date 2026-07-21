package com.limou.hrms.ai.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * 会话列表 VO
 */
@Data
public class ChatSessionVO {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 会话标题（取第一条用户消息的前20字符）
     */
    private String title;

    /**
     * 最后一条消息时间
     */
    private Date lastMessageTime;

    /**
     * 消息数量
     */
    private Integer messageCount;
}
