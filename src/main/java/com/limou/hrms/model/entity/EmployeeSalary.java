package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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

    private Long employee_id;

    private Long account_id;

    private BigDecimal base_salary;

    private BigDecimal allowance_base;

    private BigDecimal social_security_base;

    private BigDecimal housing_fund_base;

    private BigDecimal performance_base;

    private Date effective_date;

    @TableLogic(value = "0", delval = "1")
    private Integer is_deleted;

    private Date create_time;

    private Date update_time;

    private static final long serialVersionUID = 1L;
}
