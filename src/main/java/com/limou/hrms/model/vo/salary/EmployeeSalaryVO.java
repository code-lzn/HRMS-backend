package com.limou.hrms.model.vo.salary;

import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 员工薪资档案VO
 */
@Data
public class EmployeeSalaryVO {

    private Long id;

    private Long employee_id;

    private Long account_id;

    private String account_name;

    private BigDecimal base_salary;

    private BigDecimal allowance_base;

    private BigDecimal social_security_base;

    private BigDecimal housing_fund_base;

    private BigDecimal performance_base;

    private Date effective_date;

    private Date create_time;

    private Date update_time;
}
