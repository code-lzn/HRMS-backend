package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class TransferVO implements Serializable {

    private Long id;
    private String businessNo;
    private Long flowId;
    private Long recordId;
    private Long employeeId;
    private String employeeName;
    private String employeeNo;
    private Long approverId;
    private String approverName;
    private Long fromDeptId;
    private String fromDeptName;
    private Long toDeptId;
    private String toDeptName;
    private Long toPositionId;
    private String toPositionName;
    private String toRankCode;
    private Long toReporterId;
    private String toReporterName;
    private String workLocation;
    private String employmentType;
    private BigDecimal salaryAdjustment;
    private String reason;
    private String status;
    private Long operatorId;
    private String operatorName;
    private Date effectiveDate;
    private String remark;
    private Date createTime;
    private Date updateTime;

    private String approvalStatus;
    private String approvalProgress;

    private static final long serialVersionUID = 1L;
}
