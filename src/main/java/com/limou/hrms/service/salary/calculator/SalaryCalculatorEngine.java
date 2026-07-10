package com.limou.hrms.service.salary.calculator;

import com.limou.hrms.model.enums.SalaryItemTypeEnum;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 薪资计算引擎：统一调度所有工资项计算器
 */
@Component
public class SalaryCalculatorEngine {

    @Resource
    private List<SalaryItemCalculator> calculators;

    private final Map<SalaryItemTypeEnum, SalaryItemCalculator> calculatorMap = new EnumMap<>(SalaryItemTypeEnum.class);

    @PostConstruct
    public void init() {
        for (SalaryItemCalculator calculator : calculators) {
            calculatorMap.put(calculator.getItemType(), calculator);
        }
    }

    /**
     * 计算指定类型的工资项金额
     */
    public BigDecimal calculate(SalaryItemTypeEnum itemType, SalaryCalculationContext ctx) {
        SalaryItemCalculator calculator = calculatorMap.get(itemType);
        if (calculator == null) {
            return BigDecimal.ZERO;
        }
        return calculator.calculate(ctx);
    }

    /**
     * 计算所有类型的工资项（不含个税，个税需要跨月累计，单独计算）
     */
    public Map<SalaryItemTypeEnum, BigDecimal> calculateAllExceptTax(SalaryCalculationContext ctx) {
        Map<SalaryItemTypeEnum, BigDecimal> results = new EnumMap<>(SalaryItemTypeEnum.class);
        for (SalaryItemTypeEnum type : SalaryItemTypeEnum.values()) {
            if (type == SalaryItemTypeEnum.INCOME_TAX) {
                continue; // 个税单独处理
            }
            results.put(type, calculate(type, ctx));
        }
        return results;
    }

    /**
     * 获取所有计算器（用于获取工资项名称等元信息）
     */
    public List<SalaryItemCalculator> getAllCalculators() {
        return new ArrayList<>(calculators);
    }

    /**
     * 获取某个类型的计算器
     */
    public SalaryItemCalculator getCalculator(SalaryItemTypeEnum itemType) {
        return calculatorMap.get(itemType);
    }
}
