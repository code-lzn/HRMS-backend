package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 工资条详情视图
 */
@Data
public class SalarySlipDetailVO implements Serializable {

    /** 工资条ID */
    private Long id;

    /** 薪资月份: YYYY-MM */
    private String salaryMonth;

    /** 员工姓名 */
    private String employeeName;

    /** 工号 */
    private String employeeNo;

    // ===== 收入项 =====

    /** 基本工资 */
    private BigDecimal baseSalary;

    /** 岗位津贴 */
    private BigDecimal allowance;

    /** 绩效奖金 */
    private BigDecimal performanceBonus;

    /** 加班费 */
    private BigDecimal overtimePay;

    /** 手动调整金额 */
    private BigDecimal manualAdjust;

    /** 手动调整原因 */
    private String adjustReason;

    /** 应发工资 */
    private BigDecimal grossSalary;

    // ===== 扣除项 =====

    /** 迟到扣款 */
    private BigDecimal lateDeduction;

    /** 请假扣款 */
    private BigDecimal leaveDeduction;

    /** 养老保险 */
    private BigDecimal socialPension;

    /** 医疗保险 */
    private BigDecimal socialMedical;

    /** 失业保险 */
    private BigDecimal socialUnemployment;

    /** 住房公积金 */
    private BigDecimal housingFund;

    /** 个人所得税 */
    private BigDecimal incomeTax;

    /** 应扣合计 */
    private BigDecimal totalDeduction;

    // ===== 实际发放 =====

    /** 实发工资 */
    private BigDecimal netSalary;

    private static final long serialVersionUID = 1L;
}
