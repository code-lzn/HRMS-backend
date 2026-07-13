package com.limou.hrms.model.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 考勤记录视图
 */
@Data
public class AttendanceVO implements Serializable {

    /** 记录ID */
    private Long id;

    /** 考勤日期 */
    private Date attendanceDate;

    /** 上班打卡时间 */
    private Date punchInTime;

    /** 下班打卡时间 */
    private Date punchOutTime;

    /** 状态：0=正常 1=迟到 2=早退 3=缺卡 4=请假 5=旷工 */
    private Integer status;

    /** 状态文本 */
    private String statusText;

    /** 上班打卡方式 */
    private Integer punchInType;

    /** 下班打卡方式 */
    private Integer punchOutType;

    /** 备注 */
    private String remark;

    private static final long serialVersionUID = 1L;
}
