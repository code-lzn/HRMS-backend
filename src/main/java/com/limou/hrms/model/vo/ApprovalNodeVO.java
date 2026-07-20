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
    /** 是否转交（true=转交，false=委托） */
    private Boolean transferred;
    private Integer status;
    private String statusDesc;
    private String comment;
    private LocalDateTime operateTime;
}
