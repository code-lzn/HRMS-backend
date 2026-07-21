package com.limou.hrms.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 意图路由映射表
 * @TableName ai_intent_route
 */
@TableName(value = "ai_intent_route")
@Data
public class AiIntentRoute implements Serializable {

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 意图名称：leave_query/leave_action/salary_query等
     */
    @TableField("intent_name")
    private String intentName;

    /**
     * 触发关键词，逗号分隔
     */
    @TableField("keywords")
    private String keywords;

    /**
     * 前端路由路径
     */
    @TableField("route_path")
    private String routePath;

    /**
     * 路由显示名称
     */
    @TableField("route_label")
    private String routeLabel;

    /**
     * 响应类型：route_only/answer_route/both
     */
    @TableField("response_type")
    private String responseType;

    /**
     * 优先级，数值越大越优先
     */
    @TableField("priority")
    private Integer priority;

    /**
     * 状态：1=启用 0=停用
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
