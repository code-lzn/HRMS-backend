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
 * 入职申请表
 */
@TableName(value = "onboarding_application")
@Data
public class OnboardingApplication implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("gender")
    private Integer gender;

    @TableField("phone")
    private String phone;

    @TableField("email")
    private String email;

    @TableField("id_card")
    private String idCard;

    @TableField("expected_hire_date")
    private Date expectedHireDate;

    @TableField("department_id")
    private Long departmentId;

    @TableField("position_id")
    private Long positionId;

    @TableField("hire_type")
    private Integer hireType;

    @TableField("probation_months")
    private Integer probationMonths;

    @TableField("probation_ratio")
    private BigDecimal probationRatio;

    @TableField("direct_report_id")
    private Long directReportId;

    @TableField("status")
    private Integer status;

    @TableField("reject_reason")
    private String rejectReason;

    @TableField("employee_id")
    private Long employeeId;

    @TableField("actual_hire_date")
    private Date actualHireDate;

    @TableField("create_by")
    private Long createBy;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
