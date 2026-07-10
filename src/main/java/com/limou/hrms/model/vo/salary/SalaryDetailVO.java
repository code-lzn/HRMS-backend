package com.limou.hrms.model.vo.salary;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/**
 * 薪资明细VO（预览表格用）
 */
@Data
public class SalaryDetailVO {

    private Long id;

    private Long employee_id;

    private String employee_no;

    private String employee_name;

    private String department_name;

    /**
     * 工资项明细
     */
    private List<SalaryItemAmountVO> salary_items;

    private BigDecimal gross_pay;

    private BigDecimal social_security;

    private BigDecimal housing_fund;

    private BigDecimal income_tax;

    private BigDecimal total_deductions;

    private BigDecimal net_pay;

    private Integer is_abnormal;

    private String abnormal_reason;

    private BigDecimal manual_adjustment;

    private String adjustment_reason;
}
