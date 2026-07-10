package com.limou.hrms.service.salary.calculator;

import com.limou.hrms.model.enums.SalaryItemTypeEnum;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * 固定收入计算器：基本工资 + 岗位津贴
 * 试用期按比例折算
 */
@Component
public class FixedIncomeCalculator implements SalaryItemCalculator {

    @Override
    public SalaryItemTypeEnum getItemType() {
        return SalaryItemTypeEnum.FIXED_INCOME;
    }

    @Override
    public BigDecimal calculate(SalaryCalculationContext ctx) {
        BigDecimal base = ctx.getBaseSalary() != null ? ctx.getBaseSalary() : BigDecimal.ZERO;
        BigDecimal allowance = ctx.getAllowanceBase() != null ? ctx.getAllowanceBase() : BigDecimal.ZERO;

        if (ctx.isProbation() && ctx.getProbationRatio() != null) {
            base = base.multiply(ctx.getProbationRatio());
        }
        return base.add(allowance);
    }

    @Override
    public String getItemName() {
        return "固定收入";
    }
}
