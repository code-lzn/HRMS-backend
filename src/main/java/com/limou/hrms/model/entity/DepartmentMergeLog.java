package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 部门合并日志
 */
@TableName(value = "department_merge_log")
@Data
public class DepartmentMergeLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 源部门ID（被合并） */
    @TableField("source_dept_id")
    private Long sourceDeptId;

    /** 源部门名称（快照） */
    @TableField("source_dept_name")
    private String sourceDeptName;

    /** 目标部门ID（保留） */
    @TableField("target_dept_id")
    private Long targetDeptId;

    /** 目标部门名称（快照） */
    @TableField("target_dept_name")
    private String targetDeptName;

    /** 转移员工数 */
    @TableField("transferred_employees")
    private Integer transferredEmployees;

    /** 操作人ID */
    @TableField("operator_id")
    private Long operatorId;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
