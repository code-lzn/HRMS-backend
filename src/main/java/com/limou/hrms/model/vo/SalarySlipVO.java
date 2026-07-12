package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 工资条列表项视图
 */
@Data
public class SalarySlipVO implements Serializable {

    /** 工资条ID */
    private Long id;

    /** 薪资月份: YYYY-MM */
    private String salaryMonth;

    /** 批次状态 */
    private String batchStatus;

    /** 应发工资 */
    private BigDecimal grossSalary;

    /** 应扣合计 */
    private BigDecimal totalDeduction;

    /** 实发工资 */
    private BigDecimal netSalary;

    /** 是否有异常: 0=正常, 1=预警, 2=阻断 */
    private Integer hasAnomaly;

    private static final long serialVersionUID = 1L;
}
