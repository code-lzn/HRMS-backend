package com.limou.hrms.model.vo;

import com.limou.hrms.model.entity.ApprovalDetail;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class MutationLogVO implements Serializable {

    private Long id;
    private String businessType;
    private Long businessId;
    private String businessNo;
    private Long employeeId;
    private String employeeName;
    private Long deptId;
    private String deptName;
    private Date effectDate;
    private String approvalStatus;
    private Long operatorId;
    private String operatorName;
    private Date createTime;

    private List<ApprovalDetail> approvalDetails;

    private static final long serialVersionUID = 1L;
}
