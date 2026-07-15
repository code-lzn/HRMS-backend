package com.limou.hrms.model.dto.department;

import lombok.Data;

import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 更新部门请求
 */
@Data
public class DepartmentUpdateRequest implements Serializable {

    /**
     * 部门名称
     */
    private String name;

    /**
     * 部门编码
     */
    @Pattern(regexp = "^\\d+$", message = "部门编码必须为纯数字")
    private String code;

    /**
     * 上级部门ID，传null表示设为根部门
     */
    private Long parentId;

    /**
     * 部门负责人ID，传null表示清空
     */
    private Long managerId;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 部门描述，传null表示清空
     */
    private String description;

    private static final long serialVersionUID = 1L;
}
