package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName(value = "role")
@Data
public class Role implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("roleName")
    private String roleName;

    @TableField("roleCode")
    private String roleCode;

    private String description;

    @TableField("dataScope")
    private Integer dataScope;

    private Integer status;

    private String permissions;

    @TableField("fieldPermissions")
    private String fieldPermissions;

    @TableField(value = "createTime", fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(value = "updateTime", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    @TableField("isDelete")
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}