package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 待审批列表项视图
 */
@Data
public class ApprovalPendingVO implements Serializable {

    /** 审批实例ID */
    private Long recordId;

    /** 审批明细ID（当前待处理的节点） */
    private Long detailId;

    /** 业务类型 */
    private String businessType;

    /** 业务类型文本 */
    private String businessTypeText;

    /** 业务ID */
    private Long businessId;

    /** 申请人姓名 */
    private String applicantName;

    /** 申请时间 */
    private Date applyTime;

    /** 当前节点名称 */
    private String currentNodeName;

    private static final long serialVersionUID = 1L;
}
