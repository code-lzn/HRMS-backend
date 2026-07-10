package com.limou.hrms.service.salary.tax;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 7级累进税率表
 */
@Getter
@AllArgsConstructor
public enum TaxBracket {

    BRACKET_1(new BigDecimal("0"),       new BigDecimal("36000"),     new BigDecimal("0.03"), new BigDecimal("0")),
    BRACKET_2(new BigDecimal("36000"),   new BigDecimal("144000"),    new BigDecimal("0.10"), new BigDecimal("2520")),
    BRACKET_3(new BigDecimal("144000"),  new BigDecimal("300000"),    new BigDecimal("0.20"), new BigDecimal("16920")),
    BRACKET_4(new BigDecimal("300000"),  new BigDecimal("420000"),    new BigDecimal("0.25"), new BigDecimal("31920")),
    BRACKET_5(new BigDecimal("420000"),  new BigDecimal("660000"),    new BigDecimal("0.30"), new BigDecimal("52920")),
    BRACKET_6(new BigDecimal("660000"),  new BigDecimal("960000"),    new BigDecimal("0.35"), new BigDecimal("85920")),
    BRACKET_7(new BigDecimal("960000"),  new BigDecimal("999999999"), new BigDecimal("0.45"), new BigDecimal("181920"));

    /**
     * 区间下限（含）
     */
    private final BigDecimal lowerBound;

    /**
     * 区间上限（含）
     */
    private final BigDecimal upperBound;

    /**
     * 税率
     */
    private final BigDecimal rate;

    /**
     * 速算扣除数
     */
    private final BigDecimal quickDeduction;

    /**
     * 根据应纳税所得额查找对应的税率区间
     */
    public static TaxBracket findBracket(BigDecimal taxableIncome) {
        for (TaxBracket bracket : values()) {
            if (taxableIncome.compareTo(bracket.lowerBound) > 0
                    && taxableIncome.compareTo(bracket.upperBound) <= 0) {
                return bracket;
            }
        }
        // 如果超过最大区间，返回最高税率
        return BRACKET_7;
    }
}
