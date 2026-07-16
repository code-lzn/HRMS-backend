package com.limou.hrms.model.dto.onboarding;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 更新入职草稿请求体 — 所有字段可选，仅更新传入字段
 */
@Data
public class OnboardingUpdateDTO {

    private String name;
    private Integer gender;
    private String phone;
    private String email;
    private String idCard;
    private LocalDate expectedHireDate;
    private Long departmentId;
    private Long positionId;
    private Integer hireType;
    private Integer defaultProbationMonths;
    private BigDecimal probationRatio;
    private Long directReportId;
}
