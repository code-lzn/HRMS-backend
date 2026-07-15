package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class OnboardingVO implements Serializable {

    private Long id;
    private String businessNo;
    private Long flowId;
    private Long recordId;
    private Long deptId;
    private String deptName;
    private Long positionId;
    private String positionName;
    private Date hireDate;
    private Integer probationMonth;
    private String employmentType;
    private Integer contractType;
    private Date contractExpireDate;
    private BigDecimal baseSalary;
    private BigDecimal socialInsuranceBase;
    private BigDecimal housingFundBase;
    private String bankAccount;
    private String bankName;
    private String candidateName;
    private String phone;
    private String idCard;
    private String email;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private Long employeeId;
    private Long operatorId;
    private Long approverId;
    private String approverName;
    private String remark;
    private Date createTime;
    private Date updateTime;

    private String approvalStatus;
    private String approvalProgress;

    private static final long serialVersionUID = 1L;
}
