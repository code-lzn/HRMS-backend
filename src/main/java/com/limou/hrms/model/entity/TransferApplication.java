package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 调岗申请表
 */
@TableName(value = "transfer_application")
@Data
public class TransferApplication implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("employee_id")
    private Long employeeId;

    @TableField("new_department_id")
    private Long newDepartmentId;

    @TableField("new_position_id")
    private Long newPositionId;

    @TableField("new_job_level")
    private String newJobLevel;

    @TableField("new_direct_report_id")
    private Long newDirectReportId;

    @TableField("salary_adjustment")
    private BigDecimal salaryAdjustment;

    @TableField("reason")
    private String reason;

    @TableField("status")
    private Integer status;

    @TableField("create_by")
    private Long createBy;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
