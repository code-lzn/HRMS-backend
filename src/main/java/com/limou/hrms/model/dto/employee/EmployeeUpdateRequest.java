package com.limou.hrms.model.dto.employee;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 更新员工档案请求 — 所有字段可选，不传保持原值，传 null 表示清空
 */
@Data
public class EmployeeUpdateRequest implements Serializable {

    /** 姓名 */
    private String name;

    /** 性别：1=男 2=女 */
    private Integer gender;

    /** 手机号（传 null 表示清空） */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 生日（传 null 表示清空） */
    private LocalDate birthday;

    /** 户籍地址（传 null 表示清空） */
    private String registeredAddress;

    /** 现居住地址（传 null 表示清空） */
    private String currentAddress;

    /** 紧急联系人姓名（传 null 表示清空） */
    private String emergencyContactName;

    /** 紧急联系人电话（传 null 表示清空） */
    private String emergencyContactPhone;

    private static final long serialVersionUID = 1L;
}