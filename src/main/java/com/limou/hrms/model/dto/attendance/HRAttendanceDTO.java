package com.limou.hrms.model.dto.attendance;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class HRAttendanceDTO implements Serializable {

    private Long id;
    private Long employeeId;
    private String month;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date attendanceDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date punchInTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date punchOutTime;
    private String punchInLocation;
    private String punchOutLocation;
    private Double overtimeHours;
    private String leaveType;
    private String remark;

    private static final long serialVersionUID = 1L;
}