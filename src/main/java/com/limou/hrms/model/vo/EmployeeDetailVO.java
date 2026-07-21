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

    /** 员工ID */
    private Long id;

    /** 工号 */
    private String employeeNo;

    /** 系统登录账号 */
    private String account;

    /** 在职状态码：1=试用期 2=正式 3=待离职 4=已离职 */
    private Integer status;

    /** 在职状态描述 */
    private String statusDesc;

    /** 入职日期 */
    private String hireDate;

    /** 入职类型：1=全职 2=兼职 3=实习 */
    private Integer hireType;

    /** 入职类型描述 */
    private String hireTypeDesc;

    /** 创建时间 */
    private LocalDateTime createTime;

    // ==================== 个人信息（已脱敏） ====================

    /** 个人信息（按角色脱敏） */
    private PersonalInfoVO personalInfo;

    // ==================== 工作信息 ====================

    /** 工作信息 */
    private WorkInfoVO workInfo;

    // ==================== 薪资信息（仅 HR/财务可见，其余为 null） ====================

    /** 薪资与合同信息 */
    private SalaryInfoVO salaryInfo;
}