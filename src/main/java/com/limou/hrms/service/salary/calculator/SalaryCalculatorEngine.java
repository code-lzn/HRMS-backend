package com.limou.hrms.service.salary.calculator;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 薪资计算引擎（策略调度器）
 */
@Component
@Slf4j
public class SalaryCalculatorEngine {

    private final Map<Integer, SalaryCalculator> calculatorMap = new java.util.HashMap<>();

    @PostConstruct
    public void init() {
        calculatorMap.put(1, new FixedIncomeCalculator());
        calculatorMap.put(2, new VariableIncomeCalculator());
        calculatorMap.put(3, new AttendanceDeductionCalculator());
        calculatorMap.put(4, new SocialSecurityCalculator());
        calculatorMap.put(5, new HousingFundCalculator());
        calculatorMap.put(6, new IncomeTaxCalculator());
        log.info("薪资计算引擎初始化完成，已注册 {} 个计算器", calculatorMap.size());
    }

    /**
     * 根据项目类型分发计算
     */
    public BigDecimal calculate(int itemType, SalaryCalculationContext context) {
        SalaryCalculator calculator = calculatorMap.get(itemType);
        if (calculator == null) {
            log.warn("未找到类型 {} 的计算器", itemType);
            return BigDecimal.ZERO;
        }
        return calculator.calculate(context);
    }

    /**
     * 获取计算器
     */
    public SalaryCalculator getCalculator(int itemType) {
        return calculatorMap.get(itemType);
    }
}
