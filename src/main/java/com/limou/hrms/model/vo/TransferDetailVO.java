package com.limou.hrms.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 调岗申请详情 VO（含审批进度）
 */
@Data
public class TransferDetailVO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeNo;
    // 原岗位
    private Long fromDepartmentId;
    private String fromDepartmentName;
    private Long fromPositionId;
    private String fromPositionName;
    private String fromJobLevel;
    private Long fromDirectReportId;
    private String fromDirectReportName;
    // 新岗位
    private Long toDepartmentId;
    private String toDepartmentName;
    private Long toPositionId;
    private String toPositionName;
    private String toJobLevel;
    private Long toDirectReportId;
    private String toDirectReportName;
    private BigDecimal salaryAdjustment;
    private String reason;
    private Integer status;
    private String statusDesc;
    private Long approvalInstanceId;
    private Long applicantId;
    private String applicantName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 审批进度 */
    private ApprovalInstanceVO approvalProgress;
}
