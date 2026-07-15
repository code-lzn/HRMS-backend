package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 个税累计表
 * @TableName sal_tax_cumulative
 */
@TableName(value = "sal_tax_cumulative")
@Data
public class SalTaxCumulative implements Serializable {

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 员工ID */
    @TableField("employee_id")
    private Long employeeId;

    /** 纳税年度 */
    @TableField("tax_year")
    private Integer taxYear;

    /** 纳税月份 */
    @TableField("tax_month")
    private Integer taxMonth;

    /** 累计应发工资 */
    @TableField("cumulative_gross_pay")
    private BigDecimal cumulativeGrossPay;

    /** 累计起征点（5000×月数） */
    @TableField("cumulative_threshold")
    private BigDecimal cumulativeThreshold;

    /** 累计社保扣除 */
    @TableField("cumulative_social_security")
    private BigDecimal cumulativeSocialSecurity;

    /** 累计公积金扣除 */
    @TableField("cumulative_housing_fund")
    private BigDecimal cumulativeHousingFund;

    /** 累计专项附加扣除 */
    @TableField("cumulative_special_deduction")
    private BigDecimal cumulativeSpecialDeduction;

    /** 累计应纳税所得额 */
    @TableField("cumulative_taxable_income")
    private BigDecimal cumulativeTaxableIncome;

    /** 适用税率 */
    @TableField("tax_rate")
    private BigDecimal taxRate;

    /** 速算扣除数 */
    @TableField("quick_deduction")
    private BigDecimal quickDeduction;

    /** 累计应缴个税 */
    @TableField("cumulative_tax_payable")
    private BigDecimal cumulativeTaxPayable;

    /** 累计已缴个税 */
    @TableField("cumulative_tax_paid")
    private BigDecimal cumulativeTaxPaid;

    /** 当月应缴个税 */
    @TableField("current_month_tax")
    private BigDecimal currentMonthTax;

    /** 创建时间 */
    @TableField("create_time")
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
