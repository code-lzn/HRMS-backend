package com.limou.hrms.model.dto.employee;

import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建员工档案请求
 */
@Data
public class EmployeeCreateRequest {

    // ==================== 个人信息 ====================

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotNull(message = "性别不能为空")
    @Min(value = 1, message = "性别值无效")
    @Max(value = 2, message = "性别值无效")
    private Integer gender;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "身份证号不能为空")
    private String idCard;

    // ==================== 工作信息 ====================

    @NotNull(message = "入职日期不能为空")
    private LocalDate hireDate;

    @NotNull(message = "所属部门不能为空")
    private Long departmentId;

    @NotNull(message = "职位不能为空")
    private Long positionId;

    private String jobLevel;

    private Long directReportId;

    private String workLocation;

    @NotNull(message = "入职类型不能为空")
    @Min(value = 1, message = "入职类型值无效")
    @Max(value = 3, message = "入职类型值无效")
    private Integer hireType;

    // ==================== 薪资与合同信息 ====================

    @NotNull(message = "合同类型不能为空")
    @Min(value = 1, message = "合同类型值无效")
    @Max(value = 3, message = "合同类型值无效")
    private Integer contractType;

    private LocalDate contractExpireDate;

    @NotNull(message = "试用期待遇比例不能为空")
    @DecimalMin(value = "0.8", message = "试用期待遇比例范围 0.8~1.0")
    @DecimalMax(value = "1.0", message = "试用期待遇比例范围 0.8~1.0")
    private BigDecimal probationRatio;

    @NotNull(message = "薪资账套不能为空")
    private Long salaryAccountId;

    @NotNull(message = "基本工资不能为空")
    private BigDecimal baseSalary;

    private String bankAccount;

    private String bankName;
}
