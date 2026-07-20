package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 请假申请 VO
 */
@Data
public class LeaveRequestVO implements Serializable {

    private Long id;

    private Long employeeId;

    private String employeeName;

    private String departmentName;

    private Integer leaveType;

    private String leaveTypeDesc;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private BigDecimal leaveDays;

    private String reason;

    private Long handoverEmployeeId;

    private String attachmentUrl;

    private Integer status;

    private String statusDesc;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 审批实例ID，用于跳转审批详情页 */
    private Long approvalInstanceId;

    /** 审批进度（含节点时间线），审批中/已通过/已拒绝时有值 */
    private ApprovalInstanceVO approvalProgress;

    private static final long serialVersionUID = 1L;
}
