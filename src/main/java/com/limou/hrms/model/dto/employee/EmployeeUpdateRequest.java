package com.limou.hrms.model.dto.employee;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 员工更新请求（只传需变更的字段）
 */
@Data
public class EmployeeUpdateRequest implements Serializable {

    /** 员工ID */
    private Long id;

    /** 姓名 */
    private String employeeName;

    /** 性别 */
    private Integer gender;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 部门ID */
    private Long departmentId;

    /** 职位ID */
    private Long positionId;

    /** 职级 */
    private String jobLevel;

    /** 入职日期 */
    private Date hireDate;

    /** 入职类型 */
    private Integer hireType;

    /** 录用类型 */
    private String employmentType;

    // ---- 详情表字段 ----
    private String idCard;
    private Date birthday;
    private String registeredAddress;
    private String currentAddress;
    private Long directReportId;
    private String workLocation;
    private Integer contractType;
    private Date contractExpireDate;
    private BigDecimal probationRatio;
    private BigDecimal baseSalary;
    private String bankAccount;
    private String bankName;
    private String emergencyContactName;
    private String emergencyContactPhone;

    private static final long serialVersionUID = 1L;
}
