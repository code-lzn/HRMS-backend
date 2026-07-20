package com.limou.hrms.model.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 薪资构成占比数据（饼图/环形图）
 */
@Data
public class SalaryCompositionVO {

    /** 项目名称，如基本工资、岗位津贴、绩效奖金等 */
    private String itemName;

    /** 金额 */
    private BigDecimal amount;
}
