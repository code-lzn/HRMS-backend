package com.limou.hrms.model.vo.salary;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 薪资趋势VO
 */
@Data
public class SalaryTrendVO {

    private String month;

    private BigDecimal net_pay;
}
