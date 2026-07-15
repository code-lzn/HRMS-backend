package com.limou.hrms.service.salary.calculator;

import java.math.BigDecimal;

/**
 * 固定收入计算器（item_type=1）：基本工资 + 津贴
 */
public class FixedIncomeCalculator implements SalaryCalculator {

    @Override
    public int getItemType() {
        return 1;
    }

    @Override
    public BigDecimal calculate(SalaryCalculationContext context) {
        if (context == null || context.getEmployeeSalary() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal base = context.getEmployeeSalary().getBaseSalary() != null
                ? context.getEmployeeSalary().getBaseSalary() : BigDecimal.ZERO;
        BigDecimal allowance = context.getEmployeeSalary().getAllowanceBase() != null
                ? context.getEmployeeSalary().getAllowanceBase() : BigDecimal.ZERO;
        return base.add(allowance);
    }
}
