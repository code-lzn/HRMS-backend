package com.limou.hrms.model.vo;

import com.limou.hrms.model.entity.TransferApplication;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 调岗申请 VO（扩展关联字段）
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TransferApplicationVO extends TransferApplication {

    /**
     * 员工姓名（todo: 联表查询 employee 表）
     */
    private String employeeName;

    /**
     * 员工工号（todo: 联表查询 employee 表）
     */
    private String employeeNo;

    /**
     * 新部门名称（todo: 联表查询 department 表）
     */
    private String newDepartmentName;

    /**
     * 新职位名称（todo: 联表查询 position 表）
     */
    private String newPositionName;

    /**
     * 新直接汇报人姓名（todo: 联表查询 employee 表）
     */
    private String newDirectReportName;

    /**
     * 创建人姓名（todo: 联表查询 user 表）
     */
    private String createByName;

    private static final long serialVersionUID = 1L;
}
