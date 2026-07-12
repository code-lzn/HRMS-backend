package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 审批实例
 * @TableName approval_record
 */
@TableName(value = "approval_record")
@Data
public class ApprovalRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 审批流定义ID */
    private Long flowId;

    /** 业务类型 */
    private String businessType;

    /** 关联业务表记录ID */
    private Long businessId;

    /** 申请人ID（employeeId） */
    private Long applicantId;

    /** 申请人姓名（冗余） */
    private String applicantName;

    /** 当前审批步骤 */
    private Integer currentStep;

    /** 总步骤数 */
    private Integer totalSteps;

    /** 审批状态: APPROVING=审批中, APPROVED=已通过, REJECTED=已拒绝, WITHDRAWN=已撤回 */
    private String status;

    /** 审批完成时间 */
    private Date finishedAt;

    private Date createTime;

    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
