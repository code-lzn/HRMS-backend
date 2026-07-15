package com.limou.hrms.service.salary.calculator;

import java.math.BigDecimal;

/**
 * 变动收入计算器（item_type=2）：绩效基数 + 加班费
 */
public class VariableIncomeCalculator implements SalaryCalculator {

    @Override
    public int getItemType() {
        return 2;
    }

    @Override
    public BigDecimal calculate(SalaryCalculationContext context) {
        if (context == null || context.getEmployeeSalary() == null) {
            return BigDecimal.ZERO;
        }
        // 绩效工资
        BigDecimal performance = context.getEmployeeSalary().getPerformanceBase() != null
                ? context.getEmployeeSalary().getPerformanceBase() : BigDecimal.ZERO;

        // 加班费 = 小时工资 × 加班小时数
        BigDecimal overtime = BigDecimal.ZERO;
        if (context.getOvertimeHours() != null && context.getOvertimeHours() > 0) {
            overtime = context.getHourlySalary()
                    .multiply(BigDecimal.valueOf(context.getOvertimeHours()));
        }
        return performance.add(overtime);
    }
}
