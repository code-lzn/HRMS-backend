package com.limou.hrms.service.salary.calculator;

import com.limou.hrms.model.enums.SalaryItemTypeEnum;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * 社保扣除计算器：养老8% + 医疗2% + 失业0.5% = 基数 × 10.5%
 * 返回负数表示扣除
 */
@Component
public class SocialSecurityCalculator implements SalaryItemCalculator {

    private static final BigDecimal SS_RATE = new BigDecimal("0.105");

    @Override
    public SalaryItemTypeEnum getItemType() {
        return SalaryItemTypeEnum.SOCIAL_SECURITY;
    }

    @Override
    public BigDecimal calculate(SalaryCalculationContext ctx) {
        BigDecimal base = ctx.getSocialSecurityBase() != null ? ctx.getSocialSecurityBase() : BigDecimal.ZERO;
        return base.multiply(SS_RATE).negate().setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    @Override
    public String getItemName() {
        return "社保扣除";
    }
}
