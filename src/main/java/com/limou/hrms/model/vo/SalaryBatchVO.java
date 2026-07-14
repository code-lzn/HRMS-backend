package com.limou.hrms.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 薪资核算批次视图
 */
@Data
public class SalaryBatchVO {

    private Long id;
    private String batchNo;
    private String salaryMonth;
    private String status;
    private String statusText;
    private Integer totalEmployeeCount;
    private BigDecimal totalGross;
    private BigDecimal totalDeduction;
    private BigDecimal totalNet;
    private Date paidAt;
    private Date createdAt;
}
