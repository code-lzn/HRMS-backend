package com.limou.hrms.model.dto.onboarding;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 入职确认请求
 */
@Data
public class OnboardingApplicationConfirmHireRequest implements Serializable {

    private Date actualHireDate;

    private static final long serialVersionUID = 1L;
}
