package com.limou.hrms.service.salary.calculator;

import com.limou.hrms.model.enums.SalaryItemTypeEnum;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * 公积金扣除计算器：基数 × 12%
 * 返回负数表示扣除
 */
@Component
public class HousingFundCalculator implements SalaryItemCalculator {

    private static final BigDecimal HF_RATE = new BigDecimal("0.12");

    @Override
    public SalaryItemTypeEnum getItemType() {
        return SalaryItemTypeEnum.HOUSING_FUND;
    }

    @Override
    public BigDecimal calculate(SalaryCalculationContext ctx) {
        BigDecimal base = ctx.getHousingFundBase() != null ? ctx.getHousingFundBase() : BigDecimal.ZERO;
        return base.multiply(HF_RATE).negate().setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    @Override
    public String getItemName() {
        return "公积金扣除";
    }
}
