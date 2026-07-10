package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 个税累计表
 */
@TableName(value = "income_tax_cumulative")
@Data
public class IncomeTaxCumulative implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long employee_id;

    private Integer tax_year;

    private Integer tax_month;

    private BigDecimal cumulative_gross_pay;

    private BigDecimal cumulative_threshold;

    private BigDecimal cumulative_social_security;

    private BigDecimal cumulative_housing_fund;

    private BigDecimal cumulative_special_deduction;

    private BigDecimal cumulative_taxable_income;

    private BigDecimal tax_rate;

    private BigDecimal quick_deduction;

    private BigDecimal cumulative_tax_payable;

    private BigDecimal cumulative_tax_paid;

    private BigDecimal current_month_tax;

    private Date create_time;

    private static final long serialVersionUID = 1L;
}
