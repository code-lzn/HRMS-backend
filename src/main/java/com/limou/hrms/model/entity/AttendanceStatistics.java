package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("attendance_statistics")
@Data
public class AttendanceStatistics implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long employeeId;

    private Integer statYear;

    private Integer statMonth;

    private BigDecimal scheduledDays;

    private BigDecimal actualDays;

    private Integer lateCount;

    private Integer earlyLeaveCount;

    private BigDecimal absentDays;

    private BigDecimal leaveDays;

    private BigDecimal annualLeaveDays;

    private BigDecimal sickLeaveDays;

    private BigDecimal personalLeaveDays;

    private BigDecimal marriageLeaveDays;

    private BigDecimal maternityLeaveDays;

    private BigDecimal funeralLeaveDays;

    private BigDecimal compTimeLeaveDays;

    private BigDecimal overtimeHours;

    private BigDecimal annualLeaveBalance;

    private BigDecimal compTimeBalance;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}