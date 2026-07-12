package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 薪资趋势图数据项
 */
@Data
public class SalaryTrendVO implements Serializable {

    /** 薪资月份: YYYY-MM */
    private String month;

    /** 实发工资 */
    private BigDecimal netSalary;

    /** 应发工资 */
    private BigDecimal grossSalary;

    private static final long serialVersionUID = 1L;
}
