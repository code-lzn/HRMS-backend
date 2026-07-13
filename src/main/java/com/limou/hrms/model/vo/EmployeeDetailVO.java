package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 员工详情 VO（四分区嵌套）
 */
@Data
public class EmployeeDetailVO implements Serializable {

    // 基础信息
    private Long id;
    private String employeeNo;
    private String account;
    private Integer status;
    private String statusDesc;
    private Date hireDate;
    private Date createTime;

    // 个人信息
    private String employeeName;
    private Integer gender;
    private String genderDesc;
    private String phone;
    private String email;
    private String idCard;
    private Date birthday;
    private String registeredAddress;
    private String currentAddress;

    // 工作信息
    private Long departmentId;
    private String departmentName;
    private Long positionId;
    private String positionName;
    private String jobLevel;
    private Long directReportId;
    private String directReportName;
    private String workLocation;
    private Integer hireType;
    private String employmentType;
    private String employmentTypeDesc;

    // 薪资合同
    private Integer contractType;
    private String contractTypeDesc;
    private Date contractExpireDate;
    private BigDecimal probationRatio;
    private BigDecimal baseSalary;
    private String bankAccount;
    private String bankName;

    // 紧急联系人
    private String emergencyContactName;
    private String emergencyContactPhone;

    private static final long serialVersionUID = 1L;
}
