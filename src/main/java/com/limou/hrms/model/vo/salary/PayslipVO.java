package com.limou.hrms.model.vo.salary;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/**
 * 工资条VO（员工视角）
 */
@Data
public class PayslipVO {

    private String salary_month;

    private String employee_name;

    private String employee_no;

    private String department_name;

    /**
     * 收入项
     */
    private List<SalaryItemAmountVO> income_items;

    /**
     * 扣除项
     */
    private List<SalaryItemAmountVO> deduction_items;

    /**
     * 应发小计
     */
    private BigDecimal gross_pay;

    /**
     * 应扣小计
     */
    private BigDecimal total_deductions;

    /**
     * 实发金额
     */
    private BigDecimal net_pay;
}
