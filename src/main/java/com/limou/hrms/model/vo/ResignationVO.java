package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class ResignationVO implements Serializable {

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
    private Date resignDate;
    private String resignReasonType;
    private String resignType;
    private String detailReason;
    private Long handoverPersonId;
    private String handoverPersonName;
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
