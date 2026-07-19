package com.limou.hrms.model.dto.attendance;

import lombok.Data;

import java.io.Serializable;

@Data
public class OvertimeApplyRequest implements Serializable {

    /** 加班日期 yyyy-MM-dd */
    private String overtimeDate;

    /** 开始时间 yyyy-MM-dd HH:mm */
    private String startTime;

    /** 结束时间 yyyy-MM-dd HH:mm */
    private String endTime;

    /** 加班时长（小时） */
    private Double overtimeHours;

    /** 类型：0=工作日 1=休息日 2=节假日 */
    private Integer overtimeType;

    /** 加班原因 */
    private String reason;

    private static final long serialVersionUID = 1L;
}
