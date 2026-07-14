package com.limou.hrms.model.dto.role;

import lombok.Data;
import java.io.Serializable;

@Data
public class RoleAssignRequest implements Serializable {

    private Long userId;

    private Long roleId;

    private static final long serialVersionUID = 1L;
}