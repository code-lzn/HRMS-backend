package com.limou.hrms.model.dto.role;

import com.limou.hrms.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class RoleQueryRequest extends PageRequest implements Serializable {

    private Long id;

    private String roleName;

    private String roleCode;

    private Integer dataScope;

    private Integer status;

    private static final long serialVersionUID = 1L;
}