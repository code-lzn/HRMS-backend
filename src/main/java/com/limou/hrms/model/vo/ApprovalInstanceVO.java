package com.limou.hrms.model.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 审批实例详情 VO（含完整节点时间线）
 */
@Data
public class ApprovalInstanceVO {
    private Long instanceId;
    private String bizType;
    private String bizTypeDesc;
    private String title;
    private Integer status;
    private String statusDesc;
    private Long applicantId;
    private String applicantName;
    private Integer currentNodeOrder;
    /** 审批节点时间线 */
    private List<ApprovalNodeVO> nodes;
    private LocalDateTime createTime;
}
