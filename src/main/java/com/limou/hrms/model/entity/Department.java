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
 * 部门
 */
@TableName(value = "department")
@Data
public class Department implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 部门名称 */
    private String name;

    /** 部门编码，2位，用于工号生成 */
    private String code;

    /** 上级部门ID，空表示根部门 */
    @TableField("parent_id")
    private Long parentId;

    /** 部门负责人ID */
    @TableField("manager_id")
    private Long managerId;

    /** 排序序号，越小越靠前 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 部门描述 */
    private String description;

    /** 逻辑删除：0=否 1=是 */
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
