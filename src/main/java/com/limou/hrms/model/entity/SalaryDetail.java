package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 薪资明细
 */
@TableName(value = "salary_detail")
@Data
public class SalaryDetail implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long batch_id;

    private Long employee_id;

    private String salary_items;

    private BigDecimal gross_pay;

    private BigDecimal social_security;

    private BigDecimal housing_fund;

    private BigDecimal income_tax;

    private BigDecimal total_deductions;

    private BigDecimal net_pay;

    private Integer is_abnormal;

    private String abnormal_reason;

    private BigDecimal manual_adjustment;

    private String adjustment_reason;

    private Integer payslip_viewed;

    private Date create_time;

    private static final long serialVersionUID = 1L;
}
