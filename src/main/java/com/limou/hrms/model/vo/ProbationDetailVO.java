package com.limou.hrms.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 转正申请详情 VO（含审批进度）
 */
@Data
public class ProbationDetailVO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeNo;
    private String departmentName;
    private String positionName;
    private String jobLevel;
    private LocalDate probationStartDate;
    private LocalDate probationEndDate;
    private String performanceReview;
    private BigDecimal salaryAdjustment;
    private Integer result;
    private String resultDesc;
    private LocalDate extendedEndDate;
    private Integer status;
    private String statusDesc;
    private Long approvalInstanceId;
    private Long applicantId;
    private String applicantName;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 审批进度（审批中/已通过/已拒绝时返回） */
    private ApprovalInstanceVO approvalProgress;
}
