package com.limou.hrms.model.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 待办列表项 VO
 */
@Data
public class PendingItemVO {
    private Long instanceId;
    private Long nodeId;
    private String bizType;
    private String bizTypeDesc;
    private String title;
    private Long applicantId;
    private String applicantName;
    private String nodeName;
    private Integer nodeOrder;
    private String delegatorName;
    private LocalDateTime createTime;
    private LocalDateTime deadLine;
    /** 当前用户是否有权操作该节点（审批通过/拒绝） */
    private Boolean canAct;
}
