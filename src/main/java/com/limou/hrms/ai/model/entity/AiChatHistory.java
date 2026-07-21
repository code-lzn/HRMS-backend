package com.limou.hrms.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * AI对话历史
 * @TableName ai_chat_history
 */
@TableName(value = "ai_chat_history")
@Data
public class AiChatHistory implements Serializable {

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 会话UUID
     */
    @TableField("session_id")
    private String sessionId;

    /**
     * 角色：user/assistant
     */
    @TableField("role")
    private String role;

    /**
     * 消息内容
     */
    @TableField("content")
    private String content;

    /**
     * 识别到的意图
     */
    @TableField("intent")
    private String intent;

    /**
     * 推送的路由路径
     */
    @TableField("route_path")
    private String routePath;

    /**
     * 推送的路由名称
     */
    @TableField("route_label")
    private String routeLabel;

    /**
     * 用户反馈：1=点赞 0=点踩 NULL=未反馈
     */
    @TableField("feedback")
    private Integer feedback;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
