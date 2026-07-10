package com.limou.hrms.model.vo.salary;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 工资条列表VO
 */
@Data
public class PayslipListVO {

    private Long batch_id;

    private String salary_month;

    private BigDecimal gross_pay;

    private BigDecimal net_pay;

    private Integer status;

    private String status_label;

    private Integer payslip_viewed;
}
