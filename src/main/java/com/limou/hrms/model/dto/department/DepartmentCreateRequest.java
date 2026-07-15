package com.limou.hrms.model.dto.department;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 创建部门请求
 */
@Data
public class DepartmentCreateRequest implements Serializable {

    /**
     * 部门名称
     */
    @NotBlank(message = "部门名称不能为空")
    private String name;

    /**
     * 部门编码（纯数字）
     */
    @NotBlank(message = "部门编码不能为空")
    @Pattern(regexp = "^\\d+$", message = "部门编码必须为纯数字")
    private String code;

    /**
     * 上级部门ID（必填，不允许创建根部门）
     */
    @NotNull(message = "必须指定上级部门")
    private Long parentId;

    /**
     * 部门负责人ID
     */
    private Long managerId;

    /**
     * 排序序号
     */
    @NotNull(message = "排序序号不能为空")
    private Integer sortOrder;

    /**
     * 部门描述
     */
    private String description;

    private static final long serialVersionUID = 1L;
}
