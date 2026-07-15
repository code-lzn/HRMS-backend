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

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 源部门ID（被合并） */
    private Long sourceDeptId;

    /** 源部门名称（快照） */
    private String sourceDeptName;

    /** 目标部门ID（保留） */
    private Long targetDeptId;

    /** 目标部门名称（快照） */
    private String targetDeptName;

    /** 转移员工数 */
    private Integer transferredEmployees;

    /** 操作人ID */
    private Long operatorId;

    /** 创建时间 */
    private LocalDateTime createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
