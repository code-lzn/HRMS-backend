package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 考勤组列表 VO
 */
@Data
public class AttendanceGroupListVO implements Serializable {

    /** 考勤组ID */
    private Long id;

    /** 考勤组名称 */
    private String name;

    /** 班次类型：1=固定班 2=弹性班 3=排班制 */
    private Integer shiftType;

    /** 班次类型描述 */
    private String shiftTypeDesc;

    /** 上班时间 */
    private String startTime;

    /** 下班时间 */
    private String endTime;

    /** 核心工作开始时间（弹性班） */
    private String coreStartTime;

    /** 核心工作结束时间（弹性班） */
    private String coreEndTime;

    /** 每日必须工作时长（小时），仅弹性班 */
    private java.math.BigDecimal workHours;

    /** 迟到阈值（分钟） */
    private Integer lateThreshold;

    /** 早退阈值（分钟） */
    private Integer earlyLeaveThreshold;

    /** 适用规则摘要，如"技术部、产品部（2个）" */
    private String ruleSummary;

    /** 创建时间 */
    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;
}