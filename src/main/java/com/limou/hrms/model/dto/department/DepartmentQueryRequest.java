package com.limou.hrms.model.dto.department;

import com.limou.hrms.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 部门列表查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DepartmentQueryRequest extends PageRequest implements Serializable {

    /**
     * 部门名称模糊搜索
     */
    private String keyword;

    /**
     * 按上级部门筛选
     */
    private Long parentId;

    private static final long serialVersionUID = 1L;
}
