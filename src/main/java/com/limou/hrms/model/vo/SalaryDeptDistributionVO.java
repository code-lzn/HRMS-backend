package com.limou.hrms.model.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 部门薪资分布数据（柱状图）
 */
@Data
public class SalaryDeptDistributionVO {

    /** 部门名称 */
    private String departmentName;

    /** 人数 */
    private Integer employeeCount;

    /** 应发总额 */
    private BigDecimal grossTotal;

    /** 实发总额 */
    private BigDecimal netTotal;
}
