package com.limou.hrms.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * AI知识库文档
 * @TableName ai_knowledge_doc
 */
@TableName(value = "ai_knowledge_doc")
@Data
public class AiKnowledgeDoc implements Serializable {

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 文档标题
     */
    @TableField("title")
    private String title;

    /**
     * 文档正文
     */
    @TableField("content")
    private String content;

    /**
     * 分类：leave/salary/attendance/onboarding/welfare/transfer/resignation
     */
    @TableField("category")
    private String category;

    /**
     * 关联前端路由
     */
    @TableField("route_path")
    private String routePath;

    /**
     * 路由显示名称
     */
    @TableField("route_name")
    private String routeName;

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
