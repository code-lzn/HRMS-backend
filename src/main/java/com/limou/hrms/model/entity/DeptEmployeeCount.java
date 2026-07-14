package com.limou.hrms.model.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 部门员工统计结果
 */
@Data
public class DeptEmployeeCount implements Serializable {

    /**
     * 部门ID
     */
    private Long departmentId;

    /**
     * 在职员工数
     */
    private Integer cnt;

    private static final long serialVersionUID = 1L;
}