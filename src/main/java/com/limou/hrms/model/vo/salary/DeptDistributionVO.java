package com.limou.hrms.model.vo.salary;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 部门薪资分布VO（AntV分组柱状图）
 */
@Data
public class DeptDistributionVO {

    private String department_name;

    private BigDecimal total_salary;

    private BigDecimal avg_salary;

    private Integer employee_count;
}
