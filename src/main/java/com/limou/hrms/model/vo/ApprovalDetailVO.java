package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 审批详情视图
 */
@Data
public class ApprovalDetailVO implements Serializable {

    /** 审批实例ID */
    private Long recordId;

    /** 业务类型 */
    private String businessType;

    /** 业务类型文本 */
    private String businessTypeText;

    /** 业务ID */
    private Long businessId;

    /** 申请人姓名 */
    private String applicantName;

    /** 审批状态: APPROVING/APPROVED/REJECTED/WITHDRAWN */
    private String status;

    /** 审批状态文本 */
    private String statusText;

    /** 当前步骤/总步骤 */
    private Integer currentStep;

    private Integer totalSteps;

    /** 申请时间 */
    private Date applyTime;

    /** 完成时间 */
    private Date finishedAt;

    /** 审批节点历史列表 */
    private List<NodeDetail> nodeHistory;

    @Data
    public static class NodeDetail implements Serializable {
        /** 节点名称 */
        private String nodeName;
        /** 步骤序号 */
        private Integer stepOrder;
        /** 审批人姓名 */
        private String approverName;
        /** 审批动作: PENDING/APPROVE/REJECT/TRANSFER */
        private String action;
        /** 动作文本 */
        private String actionText;
        /** 审批意见 */
        private String comment;
        /** 是否代审批 */
        private Integer isDelegated;
        /** 委托人姓名 */
        private String delegatedByName;
        /** 操作时间 */
        private Date operateTime;

        private static final long serialVersionUID = 1L;
    }

    private static final long serialVersionUID = 1L;
}
