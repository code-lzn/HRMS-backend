package com.limou.hrms.model.dto.role;

import lombok.Data;
import java.io.Serializable;

@Data
public class RoleAddRequest implements Serializable {

    private String roleName;

    private String roleCode;

    private String description;

    private Integer dataScope;

    private String permissions;

    private String fieldPermissions;

    private static final long serialVersionUID = 1L;
}