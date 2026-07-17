package com.limou.hrms.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 转正申请列表项 VO
 */
@Data
public class ProbationListVO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeNo;
    private String departmentName;
    private String positionName;
    private LocalDate probationStartDate;
    private LocalDate probationEndDate;
    private BigDecimal salaryAdjustment;
    private Integer status;
    private String statusDesc;
    private String applicantName;
    private LocalDateTime createTime;
}
