package com.limou.hrms.service.salary.calculator;

import com.limou.hrms.model.enums.SalaryItemTypeEnum;
import java.math.BigDecimal;

/**
 * 工资项计算器接口（策略模式）
 */
public interface SalaryItemCalculator {

    /**
     * 获取计算器对应的工资项类型
     */
    SalaryItemTypeEnum getItemType();

    /**
     * 计算该类型的工资金额
     *
     * @param ctx 计算上下文
     * @return 计算结果金额
     */
    BigDecimal calculate(SalaryCalculationContext ctx);

    /**
     * 获取计算结果的项目名称（用于工资条展示）
     */
    String getItemName();
}
