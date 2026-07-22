package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门详情 VO（详情接口返回，含子部门）
 */
@Data
public class DepartmentVO implements Serializable {

    /** 部门ID */
    private Long id;

    /** 部门名称 */
    private String name;

    /** 部门编码 */
    private String code;

    /** 父部门ID */
    private Long parentId;

    /** 父部门名称 */
    private String parentName;

    /** 部门负责人员工ID */
    private Long managerId;

    /** 部门负责人姓名 */
    private String managerName;

    /** 排序序号 */
    private Integer sortOrder;

    /** 层级深度 */
    private Integer level;

    /** 含下属部门的在职员工总数 */
    private Integer employeeCount;

    /** 直接子部门数 */
    private Integer childCount;

    /** 直接子部门简要信息 */
    private List<DepartmentVO> children;

    /** 部门描述 */
    private String description;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}