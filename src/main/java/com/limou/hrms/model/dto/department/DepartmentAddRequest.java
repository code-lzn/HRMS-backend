package com.limou.hrms.model.dto.department;

import lombok.Data;

import java.io.Serializable;

/**
 * 部门新增请求
 */
@Data
public class DepartmentAddRequest implements Serializable {

    /** 部门名称 */
    private String name;

    /** 部门编码（2位） */
    private String code;

    /** 上级部门ID，空表示根部门 */
    private Long parentId;

    /** 部门负责人ID */
    private Long managerId;

    /** 排序序号，越小越靠前 */
    private Integer sortOrder;

    /** 部门描述 */
    private String description;

    private static final long serialVersionUID = 1L;
}
