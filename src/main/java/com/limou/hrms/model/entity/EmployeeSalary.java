package com.limou.hrms.model.entity;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 员工薪资档案
 */
@TableName(value = "employee_salary")
@Data
public class EmployeeSalary implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 员工ID，关联 employee.id
     */
    private Long employeeId;

    /**
     * 薪资账套ID，关联 salary_account.id
     */
    private Long accountId;

    /**
     * 基本工资
     */
    private BigDecimal baseSalary;

    /**
     * 津贴基数
     */
    private BigDecimal allowanceBase;

    /**
     * 社保基数
     */
    private BigDecimal socialSecurityBase;

    /**
     * 公积金基数
     */
    private BigDecimal housingFundBase;

    /**
     * 绩效基数
     */
    private BigDecimal performanceBase;

    /**
     * 生效日期
     */
    private LocalDate effectiveDate;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer isDeleted;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
