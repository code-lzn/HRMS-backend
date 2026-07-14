package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 员工列表 VO
 */
@Data
public class EmployeeVO implements Serializable {

    private Long id;
    private String employeeNo;
    private String employeeName;
    private Integer gender;
    private String phone;
    private String email;
    private Integer status;
    private String statusDesc;
    private Long departmentId;
    private String departmentName;
    private Long positionId;
    private String positionName;
    private String jobLevel;
    private String employmentType;
    private String employmentTypeDesc;
    private Date hireDate;
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
