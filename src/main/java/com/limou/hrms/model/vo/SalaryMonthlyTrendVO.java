package com.limou.hrms.model.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 薪资月度趋势数据（折线图）
 */
@Data
public class SalaryMonthlyTrendVO {

    /** 月份 */
    private String month;

    /** 应发总额 */
    private BigDecimal grossTotal;

    /** 实发总额 */
    private BigDecimal netTotal;
}
