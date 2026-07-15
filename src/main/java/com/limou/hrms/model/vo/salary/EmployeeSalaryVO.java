package com.limou.hrms.model.vo.salary;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 员工薪资档案 VO
 */
@Data
public class EmployeeSalaryVO implements Serializable {

    private Long id;

    private Long employeeId;

    private Long accountId;

    private String accountName;

    private BigDecimal baseSalary;

    private BigDecimal allowanceBase;

    private BigDecimal socialSecurityBase;

    private BigDecimal housingFundBase;

    private BigDecimal performanceBase;

    private Date effectiveDate;

    private Date createTime;

    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
