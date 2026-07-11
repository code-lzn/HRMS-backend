package com.limou.hrms.service.salary.calculator;

import com.limou.hrms.model.enums.SalaryItemTypeEnum;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * 变动收入计算器：绩效奖金（绩效基数 × 绩效系数）+ 加班费（小时工资 × 倍数 × 加班时长）
 * 倍数从 salary_item 加载，默认 1.5 倍
 */
@Component
public class VariableIncomeCalculator implements SalaryItemCalculator {

    private static final BigDecimal DEFAULT_OVERTIME_MULTIPLIER = new BigDecimal("1.5");

    @Override
    public SalaryItemTypeEnum getItemType() {
        return SalaryItemTypeEnum.VARIABLE_INCOME;
    }

    @Override
    public BigDecimal calculate(SalaryCalculationContext ctx) {
        // 绩效奖金
        BigDecimal performanceBase = ctx.getPerformanceBase() != null ? ctx.getPerformanceBase() : BigDecimal.ZERO;
        BigDecimal coefficient = ctx.getPerformanceCoefficient() != null ? ctx.getPerformanceCoefficient() : BigDecimal.ONE;
        BigDecimal performancePay = performanceBase.multiply(coefficient);

        // 加班费：小时工资 × 倍数 × 加班时长
        BigDecimal overtimePay = BigDecimal.ZERO;
        if (ctx.getOvertimeHours() > 0) {
            BigDecimal multiplier = ctx.getOvertimeMultiplier() != null
                    ? ctx.getOvertimeMultiplier() : DEFAULT_OVERTIME_MULTIPLIER;
            overtimePay = ctx.getHourlySalary()
                    .multiply(multiplier)
                    .multiply(new BigDecimal(ctx.getOvertimeHours()));
        }

        return performancePay.add(overtimePay).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    @Override
    public String getItemName() {
        return "变动收入";
    }
}
