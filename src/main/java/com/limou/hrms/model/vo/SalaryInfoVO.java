package com.limou.hrms.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 员工薪资与合同信息 VO（脱敏后，仅 HR/财务可见）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryInfoVO {

    private Integer contractType;
    private String contractTypeDesc;
    private String contractExpireDate;
    private BigDecimal probationRatio;
    private Long salaryAccountId;
    private String salaryAccountName;
    private BigDecimal baseSalary;
    private String bankAccount;
    private String bankName;
}
