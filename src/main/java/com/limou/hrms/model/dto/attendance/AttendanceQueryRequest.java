package com.limou.hrms.model.dto.attendance;

import java.io.Serializable;
import lombok.Data;

/**
 * 考勤记录查询请求
 */
@Data
public class AttendanceQueryRequest implements Serializable {

    /** 开始日期 yyyy-MM-dd */
    private String startDate;

    /** 结束日期 yyyy-MM-dd */
    private String endDate;

    /** 员工ID（管理员查询时使用） */
    private Long employeeId;

    /** 考勤状态：0=正常 1=迟到 2=早退 3=缺卡 4=请假 5=旷工 */
    private Integer status;

    /** 月份 yyyy-MM，用于日历视图 */
    private String month;

    private static final long serialVersionUID = 1L;
}
