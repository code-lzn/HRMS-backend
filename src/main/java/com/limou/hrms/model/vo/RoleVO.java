package com.limou.hrms.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class RoleVO implements Serializable {

    /** 主键ID */
    private Long id;

    /** 角色名称 */
    private String roleName;

    /** 角色编码 */
    private String roleCode;

    /** 角色描述 */
    private String description;

    /** 数据范围 */
    private Integer dataScope;

    /** 数据范围描述 */
    private String dataScopeDesc;

    /** 权限JSON */
    private String permissions;
    /** 权限编码列表 */
    private List<String> permissionCodes;
    /** 字段权限JSON */
    private String fieldPermissions;

    /** 状态 */
    private Integer status;

    private static final long serialVersionUID = 1L;
}