package com.limou.hrms.model.vo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * 审批实例详情 VO（含完整节点时间线 + 业务表单数据）
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
    /** 业务申请表数据（key-value），前端据此渲染申请详情 */
    private Map<String, Object> bizData;
    /** 审批截止时间（创建时间 + 48h） */
    private LocalDateTime deadLine;
}
