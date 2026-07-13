package com.limou.hrms.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class UserPermissionVO implements Serializable {

    private Long userId;
    private String userAccount;
    private String userName;
    private Long roleId;
    private String roleName;
    private String roleCode;
    private Integer dataScope;
    private String dataScopeDesc;
    private String permissions;
    private List<String> permissionCodes;
    private String fieldPermissions;

    private static final long serialVersionUID = 1L;
}