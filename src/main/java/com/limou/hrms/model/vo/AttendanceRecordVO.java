package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 打卡记录 VO
 */
@Data
public class AttendanceRecordVO implements Serializable {

    private Long id;

    private Long employeeId;

    private String employeeName;

    private String departmentName;

    private LocalDate attendanceDate;

    private String scheduledStartTime;

    private String scheduledEndTime;

    private LocalDateTime actualStartTime;

    private LocalDateTime actualEndTime;

    private Integer startStatus;

    private String startStatusDesc;

    private Integer endStatus;

    private String endStatusDesc;

    private static final long serialVersionUID = 1L;
}
