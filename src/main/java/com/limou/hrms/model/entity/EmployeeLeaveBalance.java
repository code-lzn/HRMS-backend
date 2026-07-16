package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@TableName(value = "employee_leave_balance")
@Data
public class EmployeeLeaveBalance implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long employeeId;

    private Integer year;

    private BigDecimal annualTotal;

    private BigDecimal annualUsed;

    private BigDecimal sickTotal;

    private BigDecimal sickUsed;

    private BigDecimal compTotal;

    private BigDecimal compUsed;

    private Date createTime;

    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
