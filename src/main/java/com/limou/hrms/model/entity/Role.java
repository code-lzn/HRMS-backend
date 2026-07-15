package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName(value = "role")
@Data
public class Role implements Serializable {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色名称 */
    @TableField("roleName")
    private String roleName;

    /** 角色编码 */
    @TableField("roleCode")
    private String roleCode;

    /** 角色描述 */
    private String description;

    /** 数据范围: 1=全量, 2=全部员工, 3=本部门及下属, 4=薪资相关, 5=仅本人 */
    @TableField("dataScope")
    private Integer dataScope;

    /** 状态: 0=禁用, 1=启用 */
    private Integer status;

    /** 权限列表（JSON数组） */
    private String permissions;

    /** 字段权限（JSON） */
    @TableField("fieldPermissions")
    private String fieldPermissions;

    /** 创建时间 */
    @TableField(value = "createTime", fill = FieldFill.INSERT)
    private Date createTime;

    /** 更新时间 */
    @TableField(value = "updateTime", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /** 逻辑删除: 0=否, 1=是 */
    @TableLogic
    @TableField("isDelete")
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}
