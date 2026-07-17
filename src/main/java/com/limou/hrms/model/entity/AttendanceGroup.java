package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName("attendance_group")
@Data
public class AttendanceGroup implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String groupName;

    private Integer shiftType;

    private Date workStartTime;

    private Date workEndTime;

    private Date lunchStartTime;

    private Date lunchEndTime;

    private Date flexibleStart;

    private Date flexibleEnd;

    private Integer lateThreshold;

    private Integer earlyThreshold;

    private String description;

    private Integer status;

    private Date createTime;

    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
