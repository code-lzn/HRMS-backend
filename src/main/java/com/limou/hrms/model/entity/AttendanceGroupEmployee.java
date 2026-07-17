package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName("attendance_group_employee")
@Data
public class AttendanceGroupEmployee implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;

    private Long employeeId;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}
