package com.limou.hrms.model.dto.transfer;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新调岗草稿请求体
 */
@Data
public class TransferUpdateDTO {

    private Long toDepartmentId;
    private Long toPositionId;
    private String toJobLevel;
    private Long toDirectReportId;
    private BigDecimal salaryAdjustment;
    private String reason;
}
