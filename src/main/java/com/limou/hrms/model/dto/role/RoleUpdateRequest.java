package com.limou.hrms.model.dto.role;

import lombok.Data;
import java.io.Serializable;

@Data
public class RoleUpdateRequest implements Serializable {

    private Long id;

    private String roleName;

    private String roleCode;

    private String description;

    private Integer dataScope;

    private Integer status;

    private String permissions;

    private String fieldPermissions;

    private static final long serialVersionUID = 1L;
}