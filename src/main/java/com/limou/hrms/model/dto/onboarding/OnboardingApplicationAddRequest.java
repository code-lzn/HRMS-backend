package com.limou.hrms.model.dto.onboarding;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 入职申请创建请求
 */
@Data
public class OnboardingApplicationAddRequest implements Serializable {

    private String name;
    private Integer gender;
    private String phone;
    private String email;
    private String idCard;
    private Date expectedHireDate;
    private Long departmentId;
    private Long positionId;
    private Integer hireType;
    private Integer probationMonths;
    private BigDecimal probationRatio;
    private Long directReportId;
    private Boolean submit;

    private static final long serialVersionUID = 1L;
}
