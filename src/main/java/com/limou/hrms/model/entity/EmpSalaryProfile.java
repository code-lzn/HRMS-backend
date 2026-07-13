package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 员工薪资档案表
 * @TableName emp_salary_profile
 */
@TableName(value ="emp_salary_profile")
@Data
public class EmpSalaryProfile implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 员工ID
     */
    private Long employeeId;

    /**
     * 适用薪资账套ID
     */
    private Long accountSetId;

    /**
     * 基本工资
     */
    private BigDecimal baseSalary;

    /**
     * 岗位津贴基数
     */
    private BigDecimal allowanceBase;

    /**
     * 绩效奖金基数
     */
    private BigDecimal performanceBase;

    /**
     * 社保缴纳基数
     */
    private BigDecimal socialInsuranceBase;

    /**
     * 公积金缴纳基数
     */
    private BigDecimal housingFundBase;

    /**
     * 试用期薪资比例 (0.80~1.00)
     */
    private BigDecimal probationSalaryRatio;

    /**
     * 银行账号（加密存储）
     */
    private String bankAccount;

    /**
     * 开户行名称
     */
    private String bankName;

    /**
     * 生效日期
     */
    private Date effectiveDate;

    /**
     * 创建时间
     */
    private Date createdTIme;

    /**
     * 更新时间
     */
    private Date updatedTime;

    /**
     * 逻辑删除：0=否 1=是
     */
    private Integer isDeleted;
}