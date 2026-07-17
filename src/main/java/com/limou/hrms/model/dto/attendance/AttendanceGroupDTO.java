package com.limou.hrms.model.dto.attendance;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class AttendanceGroupDTO implements Serializable {

    private Long id;

    private String groupName;

    private Integer shiftType;

    private Date workStartTime;

    private Date workEndTime;

    private Date lunchStartTime;

    private Date lunchEndTime;

    private Date flexibleStart;

    private Date flexibleEnd;

    private Integer lateThreshold;

    private Integer earlyThreshold;

    private String description;

    private Integer status;

    private List<Long> employeeIds;

    private static final long serialVersionUID = 1L;
}
