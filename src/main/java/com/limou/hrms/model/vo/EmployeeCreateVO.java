package com.limou.hrms.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建员工档案响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeCreateVO {

    /**
     * 员工ID
     */
    private Long id;

    /**
     * 系统自动生成的工号
     */
    private String employeeNo;

    /**
     * 系统账号（=手机号）
     */
    private String account;

    /**
     * 初始随机密码
     */
    private String initialPassword;
}
