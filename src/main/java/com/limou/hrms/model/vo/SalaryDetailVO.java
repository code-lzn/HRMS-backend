package com.limou.hrms.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 薪资明细视图（预览表格行）
 */
@Data
public class SalaryDetailVO {

    private Long id;
    private Long batchId;
    private Long employeeId;
    private String employeeNo;
    private String employeeName;
    private String departmentName;

    // 收入项
    private BigDecimal baseSalary;
    private BigDecimal allowance;
    private BigDecimal performanceBonus;
    private BigDecimal overtimePay;

    // 扣除项
    private BigDecimal lateDeduction;
    private BigDecimal leaveDeduction;
    private BigDecimal socialPension;
    private BigDecimal socialMedical;
    private BigDecimal socialUnemployment;
    private BigDecimal housingFund;
    private BigDecimal incomeTax;

    // 汇总
    private BigDecimal grossSalary;
    private BigDecimal totalDeduction;
    private BigDecimal netSalary;

    // 调整
    private BigDecimal manualAdjust;
    private String adjustReason;

    // 异常
    private Integer hasAnomaly;
    private String anomalyReason;

    private Date createdAt;
}
