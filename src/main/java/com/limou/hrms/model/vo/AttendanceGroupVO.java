package com.limou.hrms.model.vo;

import com.limou.hrms.model.dto.attendance.AttendanceGroupCreateRequest;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 考勤组 VO
 */
@Data
public class AttendanceGroupVO implements Serializable {

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

    /** 午休开始时间 */
    private String restStartTime;

    /** 午休结束时间 */
    private String restEndTime;

    /** 弹性最早打卡时间 */
    private String flexStartTime;

    /** 弹性最晚打卡时间 */
    private String flexEndTime;

    /** 迟到阈值（分钟） */
    private Integer lateThreshold;

    /** 早退阈值（分钟） */
    private Integer earlyLeaveThreshold;

    /** IP白名单 */
    private String ipWhitelist;

    /** 核心工作开始时间（弹性班） */
    private String coreStartTime;

    /** 核心工作结束时间（弹性班） */
    private String coreEndTime;

    /** 每日必须工作时长（小时），仅弹性班 */
    private java.math.BigDecimal workHours;

    /** 适用规则列表（ruleType + targetId） */
    private List<AttendanceGroupCreateRequest.RuleDTO> rules;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}