package com.limou.hrms.model.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 待转正员工 VO
 */
@Data
public class PendingEmployeeVO {
    private Long employeeId;
    private String employeeName;
    private String employeeNo;
    private String departmentName;
    private String positionName;
    private LocalDate hireDate;
    private LocalDate probationEndDate;
    private Long daysRemaining;
    private boolean hasPendingApplication;
}
