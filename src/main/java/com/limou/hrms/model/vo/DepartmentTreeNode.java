package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 部门树节点 VO
 */
@Data
public class DepartmentTreeNode implements Serializable {

    private Long id;

    private String name;

    private String code;

    private Long parentId;

    private Long managerId;

    private String managerName;

    private Integer sortOrder;

    /**
     * 含下属部门的在职员工总数
     */
    private Integer employeeCount;

    private String description;

    /**
     * 子部门列表，叶子节点返回空数组
     */
    private List<DepartmentTreeNode> children = new ArrayList<>();

    private static final long serialVersionUID = 1L;
}
