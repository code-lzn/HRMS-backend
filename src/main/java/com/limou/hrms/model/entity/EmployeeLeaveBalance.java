package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 员工假期余额实体
 */
@TableName("employee_leave_balance")
@Data
public class EmployeeLeaveBalance implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 员工ID，关联 employee.id
     */
    private Long employeeId;

    /**
     * 年份
     */
    private Integer year;

    /**
     * 假期类型：1=年假 7=调休
     */
    private Integer leaveType;

    /**
     * 总天数
     */
    private BigDecimal totalDays;

    /**
     * 已使用天数
     */
    private BigDecimal usedDays;

    /**
     * 剩余可用天数
     */
    private BigDecimal remainingDays;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
