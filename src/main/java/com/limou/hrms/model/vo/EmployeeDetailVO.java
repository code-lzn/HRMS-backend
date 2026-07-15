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
    /** 主键ID */
    private Long id;
    /** 工号 */
    private String employeeNo;
    /** 系统账号 */
    private String account;
    /** 在职状态 */
    private Integer status;
    /** 在职状态描述 */
    private String statusDesc;
    /** 入职日期 */
    private Date hireDate;
    /** 创建时间 */
    private Date createTime;

    // 个人信息
    /** 员工姓名 */
    private String employeeName;
    /** 性别 */
    private Integer gender;
    /** 性别描述 */
    private String genderDesc;
    /** 手机号 */
    private String phone;
    /** 邮箱 */
    private String email;
    /** 身份证号 */
    private String idCard;
    /** 生日 */
    private Date birthday;
    /** 户籍地址 */
    private String registeredAddress;
    /** 现居住地址 */
    private String currentAddress;

    // 工作信息
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
    /** 直接汇报人ID */
    private Long directReportId;
    /** 直接汇报人姓名 */
    private String directReportName;
    /** 工作地点 */
    private String workLocation;
    /** 录用类型 */
    private Integer hireType;
    /** 录用类型代码 */
    private String employmentType;
    /** 录用类型描述 */
    private String employmentTypeDesc;

    // 薪资合同
    /** 合同类型 */
    private Integer contractType;
    /** 合同类型描述 */
    private String contractTypeDesc;
    /** 合同到期日 */
    private Date contractExpireDate;
    /** 试用期薪资比例 */
    private BigDecimal probationRatio;
    /** 基本工资 */
    private BigDecimal baseSalary;
    /** 银行账号 */
    private String bankAccount;
    /** 开户行 */
    private String bankName;

    // 紧急联系人
    /** 紧急联系人姓名 */
    private String emergencyContactName;
    /** 紧急联系人电话 */
    private String emergencyContactPhone;

    private static final long serialVersionUID = 1L;
}
