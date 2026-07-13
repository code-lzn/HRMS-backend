package com.limou.hrms.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class RoleVO implements Serializable {

    private Long id;

    private String roleName;

    private String roleCode;

    private String description;

    private Integer dataScope;

    private String dataScopeDesc;

    private String permissions;
    private List<String> permissionCodes;
    private String fieldPermissions;

    private Integer status;

    private static final long serialVersionUID = 1L;
}