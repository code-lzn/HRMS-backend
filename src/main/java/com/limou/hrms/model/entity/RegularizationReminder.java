package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 转正提醒记录表
 */
@TableName(value = "regularization_reminder")
@Data
public class RegularizationReminder implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("employee_id")
    private Long employeeId;

    @TableField("remind_date")
    private Date remindDate;

    @TableField("is_processed")
    private Integer isProcessed;

    @TableField("create_time")
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
