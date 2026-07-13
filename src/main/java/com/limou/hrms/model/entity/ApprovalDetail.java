package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 审批明细
 * @TableName approval_detail
 */
@TableName(value = "approval_detail")
@Data
public class ApprovalDetail implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 审批实例ID */
    private Long recordId;

    /** 审批节点定义ID */
    private Long nodeId;

    /** 节点名称（快照） */
    private String nodeName;

    /** 步骤序号 */
    private Integer stepOrder;

    /** 审批人ID */
    private Long approverId;

    /** 审批人姓名（冗余） */
    private String approverName;

    /** 审批动作: PENDING/APPROVE/REJECT/TRANSFER */
    private String action;

    /** 审批意见 */
    private String comment;

    /** 是否代审批: 0=否, 1=是 */
    private Integer isDelegated;

    /** 委托人ID */
    private Long delegatedBy;

    /** 操作时间 */
    private Date operateTime;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}
