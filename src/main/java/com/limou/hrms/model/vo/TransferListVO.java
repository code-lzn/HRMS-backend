package com.limou.hrms.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 调岗申请列表项 VO
 */
@Data
public class TransferListVO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeNo;
    private String fromDepartmentName;
    private String toDepartmentName;
    private String fromPositionName;
    private String toPositionName;
    private Integer status;
    private String statusDesc;
    private String applicantName;
    private LocalDateTime createTime;
}
