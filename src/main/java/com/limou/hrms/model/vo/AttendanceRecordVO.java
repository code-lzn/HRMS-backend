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

    /** 记录ID */
    private Long id;

    /** 员工ID */
    private Long employeeId;

    /** 员工姓名 */
    private String employeeName;

    /** 部门名称 */
    private String departmentName;

    /** 考勤日期 */
    private LocalDate attendanceDate;

    /** 应上班时间，来自考勤组 start_time（弹性班则取 core_start_time） */
    private String scheduledStartTime;

    /** 应下班时间，来自考勤组 end_time（弹性班则取 core_end_time） */
    private String scheduledEndTime;

    /** 实际上班打卡时间 */
    private LocalDateTime actualStartTime;

    /** 实际下班打卡时间 */
    private LocalDateTime actualEndTime;

    /** 上班状态码：1=正常 2=迟到 3=旷工半天 4=缺卡 */
    private Integer startStatus;

    /** 上班状态中文描述 */
    private String startStatusDesc;

    /** 下班状态码：1=正常 2=早退 3=旷工半天 4=缺卡 */
    private Integer endStatus;

    /** 考勤组班次类型：1=固定班 2=弹性班 3=排班制 */
    private Integer shiftType;

    /** 下班状态中文描述 */
    private String endStatusDesc;

    private static final long serialVersionUID = 1L;
}