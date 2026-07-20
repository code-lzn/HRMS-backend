package com.limou.hrms.model.dto.employee;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 更新员工档案请求 — 所有字段可选，不传保持原值，传 null 表示清空
 */
@Data
public class EmployeeUpdateRequest implements Serializable {

    private String name;

    private Integer gender;

    private String phone;

    private String email;

    private LocalDate birthday;

    private String registeredAddress;

    private String currentAddress;

    private String emergencyContactName;

    private String emergencyContactPhone;

    private static final long serialVersionUID = 1L;
}