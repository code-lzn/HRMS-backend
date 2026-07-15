package com.limou.hrms.service.salary.calculator;

import java.math.BigDecimal;

/**
 * 薪资计算器接口（策略模式）
 */
public interface SalaryCalculator {

    /**
     * 支持的工资项目类型
     */
    int getItemType();

    /**
     * 执行计算
     */
    BigDecimal calculate(SalaryCalculationContext context);
}
