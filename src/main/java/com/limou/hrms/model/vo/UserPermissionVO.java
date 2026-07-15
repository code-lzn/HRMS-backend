package com.limou.hrms.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class UserPermissionVO implements Serializable {

    /** 用户ID */
    private Long userId;
    /** 用户账号 */
    private String userAccount;
    /** 用户昵称 */
    private String userName;
    /** 角色ID */
    private Long roleId;
    /** 角色名称 */
    private String roleName;
    /** 角色编码 */
    private String roleCode;
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

    private static final long serialVersionUID = 1L;
}