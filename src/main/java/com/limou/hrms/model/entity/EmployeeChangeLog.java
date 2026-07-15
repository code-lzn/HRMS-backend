package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 员工档案变更日志
 */
@TableName(value = "employee_change_log")
@Data
public class EmployeeChangeLog implements Serializable {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 员工ID */
    private Long employeeId;

    /** 变更字段名 */
    private String fieldName;

    /** 变更前值 */
    private String oldValue;

    /** 变更后值 */
    private String newValue;

    /** 变更类型：DIRECT_EDIT/FLOW_CHANGE/SYSTEM */
    private String changeType;

    /** 操作人ID */
    private Long operatorId;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
