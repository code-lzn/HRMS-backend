package com.limou.hrms.model.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ResignationDetailVO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeNo;
    private String departmentName;
    private String positionName;
    private LocalDate resignationDate;
    private Integer resignationType;
    private String resignationTypeDesc;
    private String reason;
    private Long handoverToId;
    private String handoverToName;
    private Integer status;
    private String statusDesc;
    private Long approvalInstanceId;
    private LocalDate actualResignationDate;
    private Long applicantId;
    private String applicantName;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private ApprovalInstanceVO approvalProgress;
}
