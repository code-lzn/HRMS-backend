package com.limou.hrms.model.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 入职申请列表项 VO
 */
@Data
public class OnboardingListVO {
    private Long id;
    private String name;
    private String phone;
    private String departmentName;
    private String positionName;
    private LocalDate expectedHireDate;
    private Integer status;
    private String statusDesc;
    private String applicantName;
    private LocalDateTime createTime;
}
