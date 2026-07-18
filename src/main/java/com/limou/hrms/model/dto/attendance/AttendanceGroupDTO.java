package com.limou.hrms.model.dto.attendance;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class AttendanceGroupDTO implements Serializable {

    private Long id;

    private String groupName;

    private Integer shiftType;

    private String workStartTime;

    private String workEndTime;

    private String lunchStartTime;

    private String lunchEndTime;

    private String flexibleStart;

    private String flexibleEnd;

    private Integer lateThreshold;

    private Integer earlyThreshold;

    private String description;

    private Integer status;

    private List<Long> employeeIds;

    private List<Long> departmentIds;

    private static final long serialVersionUID = 1L;
}
