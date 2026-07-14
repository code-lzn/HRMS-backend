package com.limou.hrms.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 员工薪资档案视图
 */
@Data
public class EmployeeSalaryVO {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeNo;
    private String departmentName;
    private Long accountSetId;
    private String accountName;
    private BigDecimal baseSalary;
    private BigDecimal allowanceBase;
    private BigDecimal performanceBase;
    private BigDecimal socialInsuranceBase;
    private BigDecimal housingFundBase;
    private BigDecimal probationSalaryRatio;
    private String bankAccount;
    private String bankName;
    private Date effectiveDate;
    private Date createdTIme;
    private Date updatedTime;
}
