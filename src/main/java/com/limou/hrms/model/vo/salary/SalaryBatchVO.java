package com.limou.hrms.model.vo.salary;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 薪资批次 VO
 */
@Data
public class SalaryBatchVO implements Serializable {

    private Long id;

    private String batchNo;

    private String salaryMonth;

    private Integer status;

    private String statusLabel;

    private Integer totalEmployees;

    private BigDecimal totalGrossPay;

    private BigDecimal totalNetPay;

    private BigDecimal totalTax;

    private Long createBy;

    private Date createTime;

    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
