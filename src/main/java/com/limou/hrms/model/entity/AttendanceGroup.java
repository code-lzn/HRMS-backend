package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 考勤组实体
 */
@TableName("attendance_group")
@Data
public class AttendanceGroup implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 考勤组名称
     */
    private String name;

    /**
     * 班次类型：1=固定班 2=弹性班 3=排班制
     */
    private Integer shiftType;

    /**
     * 上班时间
     */
    private LocalTime startTime;

    /**
     * 下班时间
     */
    private LocalTime endTime;

    /**
     * 午休开始时间
     */
    private LocalTime restStartTime;

    /**
     * 午休结束时间
     */
    private LocalTime restEndTime;

    /**
     * 弹性最早打卡时间（弹性班适用）
     */
    private LocalTime flexStartTime;

    /**
     * 弹性最晚打卡时间（弹性班适用）
     */
    private LocalTime flexEndTime;

    /**
     * 迟到阈值（分钟）
     */
    private Integer lateThreshold;

    /**
     * 早退阈值（分钟）
     */
    private Integer earlyLeaveThreshold;

    /**
     * IP白名单，逗号分隔
     */
    private String ipWhitelist;

    /**
     * 核心工作开始时间（弹性班适用）
     */
    private LocalTime coreStartTime;

    /**
     * 核心工作结束时间（弹性班适用）
     */
    private LocalTime coreEndTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0=否 1=是
     */
    @TableLogic
    private Integer isDeleted;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
