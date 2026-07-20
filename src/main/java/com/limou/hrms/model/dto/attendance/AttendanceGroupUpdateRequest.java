package com.limou.hrms.model.dto.attendance;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.io.Serializable;
import java.util.List;

/**
 * 更新考勤组请求（部分更新，传 null 表示不更新）
 */
@Data
public class AttendanceGroupUpdateRequest implements Serializable {

    /**
     * 考勤组名称
     */
    private String name;

    /**
     * 班次类型：1=固定班 2=弹性班 3=排班制
     */
    private Integer shiftType;

    /**
     * 上班时间，格式 HH:mm
     */
    private String startTime;

    /**
     * 下班时间，格式 HH:mm
     */
    private String endTime;

    /**
     * 午休开始时间
     */
    private String restStartTime;

    /**
     * 午休结束时间
     */
    private String restEndTime;

    /**
     * 弹性最早打卡时间（弹性班适用）
     */
    private String flexStartTime;

    /**
     * 弹性最晚打卡时间（弹性班适用）
     */
    private String flexEndTime;

    /**
     * 迟到阈值（分钟）
     */
    @Min(value = 0, message = "迟到阈值不能小于0")
    private Integer lateThreshold;

    /**
     * 早退阈值（分钟）
     */
    @Min(value = 0, message = "早退阈值不能小于0")
    private Integer earlyLeaveThreshold;

    /**
     * IP白名单，逗号分隔（传 null 表示清空）
     */
    private String ipWhitelist;

    /**
     * 核心工作开始时间（弹性班适用）
     */
    private String coreStartTime;

    /**
     * 核心工作结束时间（弹性班适用）
     */
    private String coreEndTime;

    /**
     * 每日必须工作时长（小时），仅弹性班使用，支持小数如 8.5
     */
    private java.math.BigDecimal workHours;

    /**
     * 适用人员规则列表（传则全量替换，重新校验权限）
     */
    @Valid
    private List<AttendanceGroupCreateRequest.RuleDTO> rules;

    private static final long serialVersionUID = 1L;
}