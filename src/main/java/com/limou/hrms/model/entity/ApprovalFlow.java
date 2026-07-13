package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 审批流定义
 * @TableName approval_flow
 */
@TableName(value = "approval_flow")
@Data
public class ApprovalFlow implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务类型: ONBOARDING/REGULARIZATION/TRANSFER/RESIGNATION/LEAVE/PATCH_CLOCK/SALARY_BATCH */
    private String businessType;

    /** 审批流名称 */
    private String flowName;

    /** 说明 */
    private String description;

    /** 状态: 1=启用, 0=禁用 */
    private Integer status;

    private Date createTime;

    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
