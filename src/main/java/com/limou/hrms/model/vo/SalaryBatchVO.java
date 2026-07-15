package com.limou.hrms.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 薪资核算批次视图
 */
@Data
public class SalaryBatchVO {

    /** 主键ID */
    private Long id;
    /** 批次号 */
    private String batchNo;
    /** 薪资月份 */
    private String salaryMonth;
    /** 状态代码 */
    private String status;
    /** 状态文本 */
    private String statusText;
    /** 核算员工总数 */
    private Integer totalEmployeeCount;
    /** 应发工资总额 */
    private BigDecimal totalGross;
    /** 扣除总额 */
    private BigDecimal totalDeduction;
    /** 实发工资总额 */
    private BigDecimal totalNet;
    /** 实际发放时间 */
    private Date paidAt;
    /** 创建时间 */
    private Date createdAt;
}
