package com.limou.hrms.service.salary.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 个税计算器（item_type=6）：中国个税累计预扣法，7级累进税率
 */
public class IncomeTaxCalculator implements SalaryCalculator {

    /**
     * 起征点：5000元/月
     */
    private static final BigDecimal MONTHLY_THRESHOLD = new BigDecimal("5000");

    @Override
    public int getItemType() {
        return 6;
    }

    @Override
    public BigDecimal calculate(SalaryCalculationContext context) {
        if (context == null || context.getEmployeeSalary() == null) {
            return BigDecimal.ZERO;
        }
        // 月度累计预扣法简化版：当月应纳税所得额 = 应发 - 5000 - 社保 - 公积金
        // 实际应查 income_tax_cumulative 取累计数据，此处简化：直接基于当月计算
        BigDecimal grossPay = BigDecimal.ZERO;
        if (context.getEmployeeSalary().getBaseSalary() != null) {
            grossPay = grossPay.add(context.getEmployeeSalary().getBaseSalary());
        }
        if (context.getEmployeeSalary().getAllowanceBase() != null) {
            grossPay = grossPay.add(context.getEmployeeSalary().getAllowanceBase());
        }
        if (context.getEmployeeSalary().getPerformanceBase() != null) {
            grossPay = grossPay.add(context.getEmployeeSalary().getPerformanceBase());
        }

        // 社保（10.5%）
        BigDecimal socialSecurity = BigDecimal.ZERO;
        if (context.getEmployeeSalary().getSocialSecurityBase() != null) {
            socialSecurity = context.getEmployeeSalary().getSocialSecurityBase()
                    .multiply(new BigDecimal("0.105"));
        }
        // 公积金（12%）
        BigDecimal housingFund = BigDecimal.ZERO;
        if (context.getEmployeeSalary().getHousingFundBase() != null) {
            housingFund = context.getEmployeeSalary().getHousingFundBase()
                    .multiply(new BigDecimal("0.12"));
        }

        // 应纳税所得额 = 应发 - 5000 - 社保 - 公积金
        BigDecimal taxableIncome = grossPay.subtract(MONTHLY_THRESHOLD)
                .subtract(socialSecurity).subtract(housingFund);
        if (taxableIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // 7级累进税率
        return calculateTax(taxableIncome).negate();
    }

    /**
     * 根据应纳税所得额计算个税
     */
    private BigDecimal calculateTax(BigDecimal taxableIncome) {
        TaxBracket bracket = findBracket(taxableIncome);
        return taxableIncome.multiply(bracket.rate)
                .subtract(bracket.quickDeduction)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 查找适用税率档位
     */
    private TaxBracket findBracket(BigDecimal taxableIncome) {
        int income = taxableIncome.intValue();
        if (income <= 36000) return new TaxBracket(new BigDecimal("0.03"), BigDecimal.ZERO);
        if (income <= 144000) return new TaxBracket(new BigDecimal("0.10"), new BigDecimal("2520"));
        if (income <= 300000) return new TaxBracket(new BigDecimal("0.20"), new BigDecimal("16920"));
        if (income <= 420000) return new TaxBracket(new BigDecimal("0.25"), new BigDecimal("31920"));
        if (income <= 660000) return new TaxBracket(new BigDecimal("0.30"), new BigDecimal("52920"));
        if (income <= 960000) return new TaxBracket(new BigDecimal("0.35"), new BigDecimal("85920"));
        return new TaxBracket(new BigDecimal("0.45"), new BigDecimal("181920"));
    }

    /**
     * 税率档位
     */
    private static class TaxBracket {
        final BigDecimal rate;
        final BigDecimal quickDeduction;

        TaxBracket(BigDecimal rate, BigDecimal quickDeduction) {
            this.rate = rate;
            this.quickDeduction = quickDeduction;
        }
    }
}
