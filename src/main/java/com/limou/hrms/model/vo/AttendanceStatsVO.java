package com.limou.hrms.model.vo;

import java.io.Serializable;
import lombok.Data;

@Data
public class AttendanceStatsVO implements Serializable {

    private Long employeeId;

    private String employeeName;

    private String departmentName;

    private String positionName;

    private Integer totalDays;

    private Integer normalDays;

    private Integer lateDays;

    private Integer earlyDays;

    private Integer missingDays;

    private Integer leaveDays;

    private Integer absentDays;

    private Double attendanceRate;

    /** 加班时长(小时) */
    private Double overtimeHours;

    /** 年假余额(天) */
    private Double annualLeaveBalance;

    /** 请假天数汇总 */
    private Integer totalLeaveDays;

    private static final long serialVersionUID = 1L;
}