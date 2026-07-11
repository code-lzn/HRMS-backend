package com.limou.hrms.model.vo;

import com.limou.hrms.model.entity.OnboardingApplication;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 入职申请 VO（扩展关联字段）
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class OnboardingApplicationVO extends OnboardingApplication {

    /**
     * 部门名称（todo: 联表查询 department 表）
     */
    private String departmentName;

    /**
     * 职位名称（todo: 联表查询 position 表）
     */
    private String positionName;

    /**
     * 直接汇报人姓名（todo: 联表查询 employee 表）
     */
    private String directReportName;

    /**
     * 创建人姓名（todo: 联表查询 user 表）
     */
    private String createByName;

    /**
     * 关联员工姓名（todo: 联表查询 employee 表）
     */
    private String employeeName;

    private static final long serialVersionUID = 1L;
}
