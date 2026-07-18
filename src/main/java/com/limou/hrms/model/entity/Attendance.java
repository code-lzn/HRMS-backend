package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 考勤打卡记录表
 */
@TableName(value ="attendance")
@Data
public class Attendance implements Serializable {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 员工ID
     */
    private Long employeeId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 考勤日期
     */
    private Date attendanceDate;

    /**
     * 上班打卡时间
     */
    private Date punchInTime;

    /**
     * 下班打卡时间
     */
    private Date punchOutTime;

    /**
     * 状态：0=正常 1=迟到 2=早退 3=缺卡 4=请假 5=旷工
     */
    private Integer status;

    /**
     * 上班打卡方式：0=网页 1=APP
     */
    private Integer punchInType;

    /**
     * 下班打卡方式：0=网页 1=APP
     */
    private Integer punchOutType;

    /**
     * 上班打卡位置
     */
    private String punchInLocation;

    /**
     * 下班打卡位置
     */
    private String punchOutLocation;

    /**
     * 迟到时长（分钟）
     */
    private Integer lateMinutes;

    /**
     * 早退时长（分钟）
     */
    private Integer earlyMinutes;

    /**
     * 加班时长（小时）
     */
    private Double overtimeHours;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 逻辑删除：0=否 1=是
     */
    @TableLogic
    private Integer isDeleted;
}
