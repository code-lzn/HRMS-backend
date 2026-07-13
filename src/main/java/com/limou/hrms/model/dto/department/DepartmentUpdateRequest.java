package com.limou.hrms.model.dto.department;

import lombok.Data;

import java.io.Serializable;

/**
 * 部门更新请求
 */
@Data
public class DepartmentUpdateRequest implements Serializable {

    /** 部门ID */
    private Long id;

    /** 部门名称 */
    private String name;

    /** 部门负责人ID */
    private Long managerId;

    /** 排序序号 */
    private Integer sortOrder;

    /** 部门描述 */
    private String description;

    private static final long serialVersionUID = 1L;
}
