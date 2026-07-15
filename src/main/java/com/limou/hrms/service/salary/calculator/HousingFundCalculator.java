package com.limou.hrms.service.salary.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 公积金扣除计算器（item_type=5）：基数 × 12%，返回负数
 */
public class HousingFundCalculator implements SalaryCalculator {

    /**
     * 公积金个人缴费比例：12%
     */
    private static final BigDecimal HOUSING_FUND_RATE = new BigDecimal("0.12");

    @Override
    public int getItemType() {
        return 5;
    }

    @Override
    public BigDecimal calculate(SalaryCalculationContext context) {
        if (context == null || context.getEmployeeSalary() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal base = context.getEmployeeSalary().getHousingFundBase() != null
                ? context.getEmployeeSalary().getHousingFundBase() : BigDecimal.ZERO;
        if (base.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return base.multiply(HOUSING_FUND_RATE)
                .setScale(2, RoundingMode.HALF_UP).negate();
    }
}
