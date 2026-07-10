package com.limou.hrms.service.salary.calculator;

import com.limou.hrms.model.enums.SalaryItemTypeEnum;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * 考勤扣款计算器：迟到扣款（50元/次）+ 请假扣款（日工资 × 请假天数）
 * 返回负数表示扣款
 */
@Component
public class AttendanceDeductionCalculator implements SalaryItemCalculator {

    private static final BigDecimal LATE_FINE_PER_TIME = new BigDecimal("50");

    @Override
    public SalaryItemTypeEnum getItemType() {
        return SalaryItemTypeEnum.ATTENDANCE_DEDUCT;
    }

    @Override
    public BigDecimal calculate(SalaryCalculationContext ctx) {
        BigDecimal lateDeduction = LATE_FINE_PER_TIME.multiply(new BigDecimal(ctx.getLateCount()));
        BigDecimal leaveDeduction = ctx.getDailySalary().multiply(new BigDecimal(ctx.getLeaveDays()));

        return lateDeduction.add(leaveDeduction).negate().setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    @Override
    public String getItemName() {
        return "考勤扣款";
    }
}
