package com.limou.hrms.model.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 调岗历史 VO
 */
@Data
public class TransferHistoryVO {
    private Long id;
    private Long employeeId;
    private String fromDepartmentName;
    private String toDepartmentName;
    private String fromPositionName;
    private String toPositionName;
    private String fromJobLevel;
    private String toJobLevel;
    private LocalDate transferDate;
    private String reason;
}
