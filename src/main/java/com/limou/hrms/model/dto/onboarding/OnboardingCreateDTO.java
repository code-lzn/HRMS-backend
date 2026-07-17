package com.limou.hrms.model.dto.onboarding;

import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建入职申请请求体
 */
@Data
public class OnboardingCreateDTO {

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

    @NotNull(message = "预计入职日期不能为空")
    private LocalDate expectedHireDate;

    @NotNull(message = "所属部门不能为空")
    private Long departmentId;

    @NotNull(message = "职位不能为空")
    private Long positionId;

    @NotNull(message = "录用类型不能为空")
    @Min(value = 1, message = "录用类型值无效")
    @Max(value = 3, message = "录用类型值无效")
    private Integer hireType;

    @NotNull(message = "试用期月数不能为空")
    @Min(value = 1, message = "试用期至少1个月")
    @Max(value = 6, message = "试用期最多6个月")
    private Integer defaultProbationMonths;

    @NotNull(message = "试用期薪资比例不能为空")
    @DecimalMin(value = "0.8", message = "试用期薪资比例范围0.8~1.0")
    @DecimalMax(value = "1.0", message = "试用期薪资比例范围0.8~1.0")
    private BigDecimal probationRatio;

    /** 直接汇报人ID（非必填，默认部门负责人） */
    private Long directReportId;

    /** 是否直接提交审批，默认false=保存草稿 */
    private Boolean submitDirectly;
}
