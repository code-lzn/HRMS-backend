package com.limou.hrms.model.vo;

import com.limou.hrms.model.dto.attendance.AttendanceGroupCreateRequest;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 考勤组 VO
 */
@Data
public class AttendanceGroupVO implements Serializable {

    private Long id;

    private String name;

    private Integer shiftType;

    private String shiftTypeDesc;

    private String startTime;

    private String endTime;

    private String restStartTime;

    private String restEndTime;

    private String flexStartTime;

    private String flexEndTime;

    private Integer lateThreshold;

    private Integer earlyLeaveThreshold;

    private String ipWhitelist;

    private BigDecimal gpsLatitude;

    private BigDecimal gpsLongitude;

    private Integer gpsRadius;

    private String coreStartTime;

    private String coreEndTime;

    /**
     * 适用规则列表
     */
    private List<AttendanceGroupCreateRequest.RuleDTO> rules;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}
