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

    private LocalDate attendanceDate;

    private Integer clockType;

    private String clockTypeDesc;

    private LocalDateTime actualTime;

    private Integer status;

    private String statusDesc;

    private String scheduledTime;

    private static final long serialVersionUID = 1L;
}
