package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 考勤组列表 VO
 */
@Data
public class AttendanceGroupListVO implements Serializable {

    private Long id;

    private String name;

    private Integer shiftType;

    private String shiftTypeDesc;

    private String startTime;

    private String endTime;

    private String coreStartTime;

    private String coreEndTime;

    private java.math.BigDecimal workHours;

    private Integer lateThreshold;

    private Integer earlyLeaveThreshold;

    /**
     * 适用规则摘要，如"技术部、产品部（2个部门）"
     */
    private String ruleSummary;

    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;
}
