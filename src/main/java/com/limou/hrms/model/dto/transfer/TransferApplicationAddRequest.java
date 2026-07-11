package com.limou.hrms.model.dto.transfer;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 调岗申请创建请求
 */
@Data
public class TransferApplicationAddRequest implements Serializable {

    private Long employeeId;
    private Long newDepartmentId;
    private Long newPositionId;
    private String newJobLevel;
    private Long newDirectReportId;
    private BigDecimal salaryAdjustment;
    private String reason;

    private static final long serialVersionUID = 1L;
}
