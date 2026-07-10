package com.limou.hrms.model.vo.salary;

import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 薪资核算批次VO
 */
@Data
public class SalaryBatchVO {

    private Long id;

    private String batch_no;

    private String salary_month;

    private Integer status;

    private String status_label;

    private Integer total_employees;

    private BigDecimal total_gross_pay;

    private BigDecimal total_net_pay;

    private BigDecimal total_tax;

    private Long create_by;

    private Date create_time;

    private Date update_time;
}
