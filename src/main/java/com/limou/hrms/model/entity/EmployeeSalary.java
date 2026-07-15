package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 员工薪资档案
 */
@TableName(value = "employee_salary")
@Data
public class EmployeeSalary implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 员工 ID → user.id
     */
    private Long employeeId;

    /**
     * 薪资账套 ID → salary_account.id
     */
    private Long accountId;

    /**
     * 月基本工资
     */
    private BigDecimal baseSalary;

    /**
     * 津贴补贴基数
     */
    private BigDecimal allowanceBase;

    /**
     * 社保缴费基数
     */
    private BigDecimal socialSecurityBase;

    /**
     * 公积金缴费基数
     */
    private BigDecimal housingFundBase;

    /**
     * 绩效工资基数
     */
    private BigDecimal performanceBase;

    /**
     * 档案生效日期
     */
    private Date effectiveDate;

    /**
     * 逻辑删除：0=正常, 1=删除
     */
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    private Date createTime;

    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
