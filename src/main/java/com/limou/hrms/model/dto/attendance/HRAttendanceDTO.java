package com.limou.hrms.model.dto.attendance;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class HRAttendanceDTO implements Serializable {

    private Long id;
    private Long employeeId;
    private String month;
    private Date attendanceDate;
    private Date punchInTime;
    private Date punchOutTime;
    private String punchInLocation;
    private String punchOutLocation;
    private Double overtimeHours;
    private String leaveType;
    private String remark;

    private static final long serialVersionUID = 1L;
}