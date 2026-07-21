package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 已审批列表项视图
 */
@Data
public class ApprovalApprovedVO implements Serializable {

    /** 审批实例ID */
    private Long recordId;

    /** 审批明细ID */
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

    /** 审批节点名称 */
    private String nodeName;

    /** 审批结果: APPROVE/REJECT/TRANSFER */
    private String action;

    /** 审批结果文本 */
    private String actionText;

    /** 审批时间 */
    private Date operateTime;

    private static final long serialVersionUID = 1L;
}
