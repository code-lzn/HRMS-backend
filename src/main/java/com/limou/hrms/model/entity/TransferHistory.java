package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 调岗历史表
 */
@TableName(value = "transfer_history")
@Data
public class TransferHistory implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("employee_id")
    private Long employeeId;

    @TableField("application_id")
    private Long applicationId;

    @TableField("old_department_id")
    private Long oldDepartmentId;

    @TableField("new_department_id")
    private Long newDepartmentId;

    @TableField("old_position_id")
    private Long oldPositionId;

    @TableField("new_position_id")
    private Long newPositionId;

    @TableField("old_job_level")
    private String oldJobLevel;

    @TableField("new_job_level")
    private String newJobLevel;

    @TableField("old_direct_report_id")
    private Long oldDirectReportId;

    @TableField("new_direct_report_id")
    private Long newDirectReportId;

    @TableField("transfer_date")
    private Date transferDate;

    @TableField("create_time")
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
