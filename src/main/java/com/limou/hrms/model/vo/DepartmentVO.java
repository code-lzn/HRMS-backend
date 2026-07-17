package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门列表/详情 VO（平铺模式）
 */
@Data
public class DepartmentVO implements Serializable {

    private Long id;

    /**
     * 部门名称
     */
    private String name;

    /**
     *
     */
    private String code;

    /**
     * 父部门 ID
     */
    private Long parentId;

    /**
     * 父部门名称
     */
    private String parentName;

    /**
     * 部门负责人 ID
     */
    private Long managerId;

    /**
     * 部门负责人名称
     */
    private String managerName;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 层级深度（1 为根部门下的第一级）
     */
    private Integer level;

    /**
     * 含下属部门的在职员工总数
     */
    private Integer employeeCount;

    /**
     * 直接子部门数
     */
    private Integer childCount;

    /**
     * 直接子部门简要信息（仅详情接口返回）
     */
    private List<DepartmentVO> children;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}
