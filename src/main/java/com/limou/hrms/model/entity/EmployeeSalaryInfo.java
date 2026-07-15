package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工薪资与合同信息实体
 */
@TableName("employee_salary_info")
@Data
public class EmployeeSalaryInfo implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 员工ID，关联 employee.id
     */
    private Long employeeId;

    /**
     * 薪资关联ID
     */
    private Long employeeSalaryId;

    /**
     * 合同类型：1=固定期限 2=无固定期限 3=劳务合同
     */
    private Integer contractType;

    /**
     * 合同到期日（固定期限合同必填）
     */
    private LocalDate contractExpireDate;

    /**
     * 试用期待遇比例，范围 0.8~1.0
     */
    private BigDecimal probationRatio;

    /**
     * 薪资账套ID，关联 salary_account.id
     */
    private Long salaryAccountId;

    /**
     * 银行账号（AES-256 加密存储）
     */
    private String bankAccount;

    /**
     * 开户行
     */
    private String bankName;

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
