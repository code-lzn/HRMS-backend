package com.limou.hrms.model.dto.department;

import lombok.Data;

import java.io.Serializable;

/**
 * 部门合并请求
 */
@Data
public class DepartmentMergeRequest implements Serializable {

    /** 源部门ID（被合并） */
    private Long sourceDeptId;

    /** 目标部门ID（保留） */
    private Long targetDeptId;

    private static final long serialVersionUID = 1L;
}
