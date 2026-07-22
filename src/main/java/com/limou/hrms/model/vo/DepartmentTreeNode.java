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

    /** 部门ID */
    private Long id;

    /** 部门名称 */
    private String name;

    /** 部门编码 */
    private String code;

    /** 父部门ID */
    private Long parentId;

    /** 部门负责人员工ID */
    private Long managerId;

    /** 部门负责人姓名 */
    private String managerName;

    /** 排序序号 */
    private Integer sortOrder;

    /** 层级深度（0=根部门） */
    private Integer level;

    /** 含下属部门的在职员工总数 */
    private Integer employeeCount;

    /** 部门描述 */
    private String description;

    /** 子部门列表，叶子节点返回空数组 */
    private List<DepartmentTreeNode> children = new ArrayList<>();

    private static final long serialVersionUID = 1L;
}