package com.limou.hrms.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 员工档案详情响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDetailVO {

    // ==================== 基础信息 ====================
    private Long id;
    private String employeeNo;
    private String account;
    private Integer status;
    private String statusDesc;
    private String hireDate;
    private Integer hireType;
    private String hireTypeDesc;
    private LocalDateTime createTime;

    // ==================== 个人信息（已脱敏） ====================
    private PersonalInfoVO personalInfo;

    // ==================== 工作信息 ====================
    private WorkInfoVO workInfo;

    // ==================== 薪资信息（仅 HR/财务可见，其余为 null） ====================
    private SalaryInfoVO salaryInfo;
}
