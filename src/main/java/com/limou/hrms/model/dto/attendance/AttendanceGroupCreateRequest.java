package com.limou.hrms.model.dto.attendance;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 创建考勤组请求
 */
@Data
public class AttendanceGroupCreateRequest implements Serializable {

    /**
     * 考勤组名称
     */
    @NotBlank(message = "考勤组名称不能为空")
    private String name;

    /**
     * 班次类型：1=固定班 2=弹性班 3=排班制
     */
    @NotNull(message = "班次类型不能为空")
    private Integer shiftType;

    /**
     * 上班时间，格式 HH:mm（弹性班可省略）
     */
    private String startTime;

    /**
     * 下班时间，格式 HH:mm（弹性班可省略）
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
     * 迟到阈值（分钟），默认 15
     */
    @Min(value = 0, message = "迟到阈值不能小于0")
    private Integer lateThreshold;

    /**
     * 早退阈值（分钟），默认 15
     */
    @Min(value = 0, message = "早退阈值不能小于0")
    private Integer earlyLeaveThreshold;

    /**
     * IP白名单，逗号分隔
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
     * 每日必须工作时长（小时），仅弹性班使用，支持小数如 8.5，默认 8
     */
    private java.math.BigDecimal workHours;

    /**
     * 适用人员规则列表
     */
    @NotEmpty(message = "适用人员规则不能为空")
    @Valid
    private List<RuleDTO> rules;

    /**
     * 适用规则 DTO
     */
    @Data
    public static class RuleDTO implements Serializable {

        /**
         * 适用类型：1=按部门 2=按职位 3=按个人
         */
        @NotNull(message = "适用类型不能为空")
        private Integer ruleType;

        /**
         * 目标ID（部门ID/职位ID/员工ID）
         */
        @NotNull(message = "目标ID不能为空")
        private Long targetId;

        private static final long serialVersionUID = 1L;
    }

    private static final long serialVersionUID = 1L;
}
