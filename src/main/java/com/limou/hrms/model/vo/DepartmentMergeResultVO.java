package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 部门合并结果
 */
@Data
public class DepartmentMergeResultVO implements Serializable {

    /** 转移员工数 */
    private Integer transferredEmployees;

    /** 转移子部门数 */
    private Integer transferredChildDepts;

    private static final long serialVersionUID = 1L;
}
