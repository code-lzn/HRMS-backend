package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 调薪历史表
 * @TableName sal_change_log
 */
@TableName(value = "sal_change_log")
@Data
public class SalChangeLog implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 员工ID */
    @TableField("employee_id")
    private Long employeeId;

    /** 变更类型：1=调薪 2=账套变更 3=基数调整 4=转正调薪 5=调岗调薪 */
    @TableField("change_type")
    private Integer changeType;

    /** 变更前薪资档案快照(JSON) */
    @TableField("old_value")
    private String oldValue;

    /** 变更后薪资档案快照(JSON) */
    @TableField("new_value")
    private String newValue;

    /** 生效日期 */
    @TableField("effective_date")
    private Date effectiveDate;

    /** 操作人ID */
    @TableField("operator_id")
    private Long operatorId;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @TableField("create_time")
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
