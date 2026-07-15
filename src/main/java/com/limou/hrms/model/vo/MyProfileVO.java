package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 我的档案完整视图（employee + employee_detail 联表）
 * <p>
 * 用于 /api/employee/profile/getMyProfile 接口返回
 */
@Data
public class MyProfileVO implements Serializable {

    // ==================== employee 主表字段 ====================

    private Long id;
    private String employeeName;
    private String employeeNo;
    private String account;
    private Integer status;
    private Integer gender;
    private Date hireDate;
    private Integer hireType;
    private String phone;
    private Long departmentId;
    private String departmentName;
    private Long positionId;
    private String positionName;
    private String email;

    // ==================== employee_detail 扩展表字段 ====================

    /** 身份证号（脱敏） */
    private String idCard;
    /** 现居住地址 */
    private String currentAddress;
    /** 紧急联系人姓名 */
    private String emergencyContactName;
    /** 紧急联系人电话（脱敏） */
    private String emergencyContactPhone;
    /** 生日 */
    private Date birthday;
    /** 户籍地址 */
    private String registeredAddress;
    /** 职级 */
    private String jobLevel;
    /** 直接汇报人ID */
    private Long directReportId;
    /** 直接汇报人姓名 */
    private String directReportName;
    /** 工作地点 */
    private String workLocation;
    /** 合同类型 */
    private Integer contractType;
    /** 合同到期日 */
    private Date contractExpireDate;
    /** 试用期待遇比例 */
    private BigDecimal probationRatio;
    /** 基本工资 */
    private BigDecimal baseSalary;
    /** 银行账号（脱敏） */
    private String bankAccount;
    /** 开户行 */
    private String bankName;

    // ==================== 状态描述 ====================

    private String statusDesc;
    private String genderDesc;
    private String contractTypeDesc;

    // ==================== 前端交互字段 ====================

    /**
     * 可编辑的字段名集合（前端据此判断字段是否可编辑）
     */
    private java.util.Set<String> editableFields;

    /**
     * 是否为敏感角色（HR/管理员可见完整明文，普通员工脱敏隐藏）
     */
    private Boolean isSensitiveRole;

    private static final long serialVersionUID = 1L;
}
