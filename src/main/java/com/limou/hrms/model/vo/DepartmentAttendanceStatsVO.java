package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class DepartmentAttendanceStatsVO implements Serializable {

    private Long departmentId;

    private String departmentName;

    private Integer employeeCount;

    private Integer totalWorkDays;

    private Integer actualAttendanceDays;

    private Integer lateCount;

    private Integer earlyCount;

    private Integer absentDays;

    private Integer leaveDays;

    private Double attendanceRate;

    private Double lateRate;

    private Double leaveRate;

    private static final long serialVersionUID = 1L;
}
