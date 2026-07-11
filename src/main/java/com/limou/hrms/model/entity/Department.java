package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 部门表
 * @TableName department
 */
@TableName(value ="department")
@Data
public class Department {
    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 部门编码（2位，用于工号生成）
     */
    private String deptCode;

    /**
     * 上级部门ID，NULL表示根部门
     */
    private Long parentId;

    /**
     * 部门负责人ID（关联员工表）
     */
    private Long managerId;

    /**
     * 排序序号（越小越靠前）
     */
    private Integer sortOrder;

    /**
     * 部门描述
     */
    private String description;

    /**
     * 创建时间
     */
    private Date createdTime;

    /**
     * 更新时间
     */
    private Date updatedTime;

    /**
     * 逻辑删除：0=否 1=是
     */
    private Integer isDeleted;
}