package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class HRAttendanceVO implements Serializable {

    private Long id;
    private Long employeeId;
    private String employeeNo;
    private String employeeName;
    private Long departmentId;
    private String deptName;
    private String month;
    private Date attendanceDate;
    private Date punchInTime;
    private Date punchOutTime;
    private Integer status;
    private String statusText;
    private Integer lateMinutes;
    private Integer earlyMinutes;
    private Double overtimeHours;
    private String leaveTypeText;
    private String punchInLocation;
    private String punchOutLocation;
    private String remark;
    private Date createTime;
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}