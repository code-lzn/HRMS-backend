package com.limou.hrms.service.salary.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 社保扣除计算器（item_type=4）：基数 × 10.5%，返回负数
 */
public class SocialSecurityCalculator implements SalaryCalculator {

    /**
     * 社保个人缴费比例：养老8% + 医疗2% + 失业0.5% = 10.5%
     */
    private static final BigDecimal SOCIAL_SECURITY_RATE = new BigDecimal("0.105");

    @Override
    public int getItemType() {
        return 4;
    }

    @Override
    public BigDecimal calculate(SalaryCalculationContext context) {
        if (context == null || context.getEmployeeSalary() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal base = context.getEmployeeSalary().getSocialSecurityBase() != null
                ? context.getEmployeeSalary().getSocialSecurityBase() : BigDecimal.ZERO;
        if (base.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return base.multiply(SOCIAL_SECURITY_RATE)
                .setScale(2, RoundingMode.HALF_UP).negate();
    }
}
