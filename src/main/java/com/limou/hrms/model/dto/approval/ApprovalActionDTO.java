package com.limou.hrms.model.dto.approval;

import lombok.Data;

/**
 * 审批操作请求参数
 */
@Data
public class ApprovalActionDTO {
    /** 审批意见（拒绝时必填） */
    private String comment;
    /** 转交目标审批人ID（转交时必填） */
    private Long toApproverId;
}
