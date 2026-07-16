package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class RegularizationVO implements Serializable {

    private Long id;
    private String businessNo;
    private Long flowId;
    private Long recordId;
    private Long employeeId;
    private String employeeName;
    private String employeeNo;
    private String deptName;
    private String positionName;
    private Long approverId;
    private String approverName;
    private Date probationStartDate;
    private Date probationEndDate;
    private String evaluation;
    private BigDecimal salaryAdjustment;
    private String adjustRemark;
    private String result;
    private Integer extendedMonths;
    private String status;
    private Long operatorId;
    private String operatorName;
    private String remark;
    private Date createTime;
    private Date updateTime;

    private String approvalStatus;
    private String approvalProgress;

    private static final long serialVersionUID = 1L;
}
