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

    private static final long serialVersionUID = 1L;
}