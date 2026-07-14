package com.limou.hrms.model.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 审批节点 VO
 */
@Data
public class ApprovalNodeVO {
    private Long nodeId;
    private String nodeName;
    private Integer nodeOrder;
    private Long approverId;
    private String approverName;
    private Long originalApproverId;
    private String originalApproverName;
    private Integer status;
    private String statusDesc;
    private String comment;
    private LocalDateTime operateTime;
}
