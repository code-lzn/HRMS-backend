package com.limou.hrms.service.salary.calculator;

import com.limou.hrms.model.enums.SalaryItemTypeEnum;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * 社保扣除计算器：基数 × 社保费率（费率从 salary_item 加载，默认 10.5%）
 * 返回负数表示扣除
 */
@Component
public class SocialSecurityCalculator implements SalaryItemCalculator {

    private static final BigDecimal DEFAULT_RATE = new BigDecimal("0.105");

    @Override
    public SalaryItemTypeEnum getItemType() {
        return SalaryItemTypeEnum.SOCIAL_SECURITY;
    }

    @Override
    public BigDecimal calculate(SalaryCalculationContext ctx) {
        BigDecimal base = ctx.getSocialSecurityBase() != null ? ctx.getSocialSecurityBase() : BigDecimal.ZERO;
        BigDecimal rate = ctx.getSocialSecurityRate() != null ? ctx.getSocialSecurityRate() : DEFAULT_RATE;
        return base.multiply(rate).negate().setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    @Override
    public String getItemName() {
        return "社保扣除";
    }
}
