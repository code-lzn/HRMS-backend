package com.limou.hrms.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 员工薪资档案视图
 */
@Data
public class EmployeeSalaryVO {

    /** 主键ID */
    private Long id;
    /** 员工ID */
    private Long employeeId;
    /** 员工姓名 */
    private String employeeName;
    /** 工号 */
    private String employeeNo;
    /** 部门名称 */
    private String departmentName;
    /** 薪资账套ID */
    private Long accountSetId;
    /** 账套名称 */
    private String accountName;
    /** 基本工资 */
    private BigDecimal baseSalary;
    /** 岗位津贴基数 */
    private BigDecimal allowanceBase;
    /** 绩效奖金基数 */
    private BigDecimal performanceBase;
    /** 社保缴纳基数 */
    private BigDecimal socialInsuranceBase;
    /** 公积金缴纳基数 */
    private BigDecimal housingFundBase;
    /** 试用期薪资比例 */
    private BigDecimal probationSalaryRatio;
    /** 银行账号 */
    private String bankAccount;
    /** 开户行 */
    private String bankName;
    /** 生效日期 */
    private Date effectiveDate;
    /** 创建时间 */
    private Date createdTIme;
    /** 更新时间 */
    private Date updatedTime;
}
