package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 员工（组织架构依赖的最小结构，后续员工模块可扩展）
 */
@TableName(value = "employee")
@Data
public class Employee implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 员工姓名 */
    private String name;

    /** 所属部门ID */
    @TableField("department_id")
    private Long departmentId;

    /** 职位ID */
    @TableField("position_id")
    private Long positionId;

    /** 员工状态：1=试用期 2=正式 3=离职 */
    private Integer status;

    /** 逻辑删除 */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
