package com.limou.hrms.model.dto.approval;

import lombok.Data;

import java.io.Serializable;

/**
 * 审批操作请求
 */
@Data
public class ApprovalActionRequest implements Serializable {

    /** 审批明细ID */
    private Long detailId;

    /** 审批意见 */
    private String comment;

    /** 转交目标人ID（仅转交时使用） */
    private Long targetUserId;

    private static final long serialVersionUID = 1L;
}
