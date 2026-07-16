package com.limou.hrms.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 入职申请详情 VO（含审批进度）
 */
@Data
public class OnboardingDetailVO {
    // 基本信息
    private Long id;
    private String name;
    private Integer gender;
    private String genderDesc;
    private String phone;
    private String email;
    private String idCard;
    private LocalDate expectedHireDate;
    private Long departmentId;
    private String departmentName;
    private Long positionId;
    private String positionName;
    private Integer hireType;
    private String hireTypeDesc;
    private Integer defaultProbationMonths;
    private BigDecimal probationRatio;
    private Long directReportId;
    private String directReportName;
    private Integer status;
    private String statusDesc;
    private Long approvalInstanceId;
    private Long employeeId;
    private Long applicantId;
    private String applicantName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 审批进度（审批中/已通过/已拒绝时返回） */
    private ApprovalInstanceVO approvalProgress;
}
