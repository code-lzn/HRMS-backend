package com.limou.hrms.service.salary.calculator;

import java.math.BigDecimal;

/**
 * 考勤扣款计算器（item_type=3）：迟到扣款 + 请假扣款，返回负数
 */
public class AttendanceDeductionCalculator implements SalaryCalculator {

    /**
     * 每次迟到扣款金额
     */
    private static final BigDecimal LATE_FINE = new BigDecimal("50");

    @Override
    public int getItemType() {
        return 3;
    }

    @Override
    public BigDecimal calculate(SalaryCalculationContext context) {
        if (context == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalDeduction = BigDecimal.ZERO;

        // 迟到扣款 = 50 × 次数
        if (context.getLateCount() != null && context.getLateCount() > 0) {
            totalDeduction = totalDeduction.add(
                    LATE_FINE.multiply(BigDecimal.valueOf(context.getLateCount())));
        }
        // 请假扣款 = 日工资 × 请假天数
        if (context.getLeaveDays() != null && context.getLeaveDays() > 0) {
            totalDeduction = totalDeduction.add(
                    context.getDailySalary().multiply(BigDecimal.valueOf(context.getLeaveDays())));
        }
        // 返回负数
        return totalDeduction.negate();
    }
}
