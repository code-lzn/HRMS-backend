package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 离职申请表
 */
@TableName(value = "resignation_application")
@Data
public class ResignationApplication implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("employee_id")
    private Long employeeId;

    @TableField("leave_date")
    private Date leaveDate;

    @TableField("leave_reason")
    private String leaveReason;

    @TableField("leave_reason_detail")
    private String leaveReasonDetail;

    @TableField("leave_type")
    private Integer leaveType;

    @TableField("handover_person_id")
    private Long handoverPersonId;

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
