package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 打卡记录实体
 */
@TableName("attendance_record")
@Data
public class AttendanceRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 员工ID，关联 employee.id
     */
    private Long employeeId;

    /**
     * 考勤日期
     */
    private LocalDate attendanceDate;

    /**
     * 当天适用的考勤组ID
     */
    private Long attendanceGroupId;

    /**
     * 规定上班时间
     */
    private LocalTime scheduledStartTime;

    /**
     * 规定下班时间
     */
    private LocalTime scheduledEndTime;

    /**
     * 实际上班打卡时间
     */
    private LocalDateTime actualStartTime;

    /**
     * 实际下班打卡时间
     */
    private LocalDateTime actualEndTime;

    /**
     * 上班状态：1=正常 2=迟到 3=旷工半天 4=缺卡
     */
    private Integer startStatus;

    /**
     * 下班状态：1=正常 2=早退 3=旷工半天 4=缺卡
     */
    private Integer endStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
