package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 部门树节点
 */
@Data
public class DepartmentTreeVO implements Serializable {

    /** 部门ID */
    private Long id;

    /** 部门名称 */
    private String name;

    /** 部门编码 */
    private String code;

    /** 上级部门ID */
    private Long parentId;

    /** 部门负责人ID */
    private Long managerId;

    /** 部门负责人姓名 */
    private String managerName;

    /** 排序序号 */
    private Integer sortOrder;

    /** 部门描述 */
    private String description;

    /** 部门人数（含子部门递归汇总） */
    private Integer employeeCount;

    /** 子部门列表 */
    private List<DepartmentTreeVO> children = new ArrayList<>();

    private static final long serialVersionUID = 1L;
}
