package com.limou.hrms.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 薪资明细视图（预览表格行）
 */
@Data
public class SalaryDetailVO {

    /** 主键ID */
    private Long id;
    /** 批次ID */
    private Long batchId;
    /** 员工ID */
    private Long employeeId;
    /** 工号 */
    private String employeeNo;
    /** 员工姓名 */
    private String employeeName;
    /** 部门名称 */
    private String departmentName;

    // 收入项
    /** 基本工资 */
    private BigDecimal baseSalary;
    /** 岗位津贴 */
    private BigDecimal allowance;
    /** 绩效奖金 */
    private BigDecimal performanceBonus;
    /** 加班费 */
    private BigDecimal overtimePay;

    // 扣除项
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

    // 汇总
    /** 应发工资 */
    private BigDecimal grossSalary;
    /** 应扣合计 */
    private BigDecimal totalDeduction;
    /** 实发工资 */
    private BigDecimal netSalary;

    // 调整
    /** 手动调整金额 */
    private BigDecimal manualAdjust;
    /** 手动调整原因 */
    private String adjustReason;

    // 异常
    /** 是否有异常 */
    private Integer hasAnomaly;
    /** 异常说明 */
    private String anomalyReason;

    /** 创建时间 */
    private Date createdAt;
}
