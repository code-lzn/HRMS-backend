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
 * 转正申请表
 */
@TableName(value = "regularization_application")
@Data
public class RegularizationApplication implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("employee_id")
    private Long employeeId;

    @TableField("probation_start_date")
    private Date probationStartDate;

    @TableField("probation_end_date")
    private Date probationEndDate;

    @TableField("performance_review")
    private String performanceReview;

    @TableField("salary_adjustment")
    private BigDecimal salaryAdjustment;

    @TableField("result")
    private Integer result;

    @TableField("extended_probation_date")
    private Date extendedProbationDate;

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
