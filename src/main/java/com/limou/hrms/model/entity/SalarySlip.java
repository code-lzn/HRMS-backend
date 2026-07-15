package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 薪资核算明细表（工资条）
 * @TableName sal_batch_detail
 */
@TableName(value = "sal_batch_detail")
@Data
public class SalarySlip implements Serializable {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 批次ID */
    private Long batchId;

    /** 员工ID */
    private Long employeeId;

    /** 基本工资 */
    private BigDecimal baseSalary;

    /** 岗位津贴 */
    private BigDecimal allowance;

    /** 绩效奖金 */
    private BigDecimal performanceBonus;

    /** 加班费 */
    private BigDecimal overtimePay;

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

    /** 应发工资 */
    private BigDecimal grossSalary;

    /** 应扣合计 */
    private BigDecimal totalDeduction;

    /** 实发工资 */
    private BigDecimal netSalary;

    /** 是否有异常: 0=正常, 1=预警, 2=阻断 */
    private Integer hasAnomaly;

    /** 异常说明 */
    private String anomalyReason;

    /** 手动调整金额 */
    private BigDecimal manualAdjust;

    /** 手动调整原因 */
    private String adjustReason;

    /** 创建时间 */
    private Date createdAt;

    /** 更新时间 */
    private Date updatedAt;

    private static final long serialVersionUID = 1L;
}
