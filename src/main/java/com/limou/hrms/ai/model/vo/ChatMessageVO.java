package com.limou.hrms.ai.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话消息 VO（SSE 流式推送的单个消息块）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageVO {

    /**
     * 消息类型：thinking/intent/content/route/done/error
     */
    private String type;

    /**
     * 文本内容
     */
    private String content;

    /**
     * 识别到的意图
     */
    private String intent;

    /**
     * 置信度 0-1
     */
    private Double confidence;

    /**
     * 前端路由路径
     */
    private String path;

    /**
     * 路由显示名称
     */
    private String label;

    /**
     * 路由动作：push=推送跳转
     */
    private String action;

    /**
     * 消息ID（用于关联反馈）
     */
    private Long messageId;
}
