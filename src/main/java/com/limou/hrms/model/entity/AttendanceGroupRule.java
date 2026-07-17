package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 考勤组适用规则实体
 */
@TableName("attendance_group_rule")
@Data
public class AttendanceGroupRule implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 考勤组ID
     */
    private Long attendanceGroupId;

    /**
     * 适用类型：1=按部门 2=按职位 3=按个人
     */
    private Integer ruleType;

    /**
     * 目标ID（部门ID/职位ID/员工ID）
     */
    private Long targetId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
