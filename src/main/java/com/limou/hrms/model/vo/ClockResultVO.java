package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 打卡结果 VO
 */
@Data
public class ClockResultVO implements Serializable {

    /** 考勤日期 */
    private LocalDate attendanceDate;

    /** 打卡类型：1=上班打卡 2=下班打卡 */
    private Integer clockType;

    /** 打卡类型描述：上班打卡 / 下班打卡 */
    private String clockTypeDesc;

    /** 实际打卡时间 */
    private LocalDateTime actualTime;

    /** 打卡状态码：1=正常 2=迟到/早退 3=旷工半天 */
    private Integer status;

    /** 打卡状态描述 */
    private String statusDesc;

    /** 应打卡时间（弹性班取 flexStart/flexEnd，固定班取 startTime/endTime） */
    private String scheduledTime;

    private static final long serialVersionUID = 1L;
}