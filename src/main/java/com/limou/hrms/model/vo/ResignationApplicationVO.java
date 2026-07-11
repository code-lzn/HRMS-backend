package com.limou.hrms.model.vo;

import com.limou.hrms.model.entity.ResignationApplication;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 离职申请 VO（扩展关联字段）
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ResignationApplicationVO extends ResignationApplication {

    /**
     * 员工姓名（todo: 联表查询 employee 表）
     */
    private String employeeName;

    /**
     * 员工工号（todo: 联表查询 employee 表）
     */
    private String employeeNo;

    /**
     * 部门名称（todo: 联表查询 employee + department 表）
     */
    private String departmentName;

    /**
     * 交接人姓名（todo: 联表查询 employee 表）
     */
    private String handoverPersonName;

    /**
     * 创建人姓名（todo: 联表查询 user 表）
     */
    private String createByName;

    private static final long serialVersionUID = 1L;
}
