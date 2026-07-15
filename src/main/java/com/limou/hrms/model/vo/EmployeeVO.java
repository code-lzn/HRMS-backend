package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 员工列表 VO
 */
@Data
public class EmployeeVO implements Serializable {

    /** 主键ID */
    private Long id;
    /** 工号 */
    private String employeeNo;
    /** 员工姓名 */
    private String employeeName;
    /** 性别 */
    private Integer gender;
    /** 手机号 */
    private String phone;
    /** 邮箱 */
    private String email;
    /** 在职状态 */
    private Integer status;
    /** 在职状态描述 */
    private String statusDesc;
    /** 部门ID */
    private Long departmentId;
    /** 部门名称 */
    private String departmentName;
    /** 职位ID */
    private Long positionId;
    /** 职位名称 */
    private String positionName;
    /** 职级 */
    private String jobLevel;
    /** 录用类型代码 */
    private String employmentType;
    /** 录用类型描述 */
    private String employmentTypeDesc;
    /** 入职日期 */
    private Date hireDate;
    /** 创建时间 */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
