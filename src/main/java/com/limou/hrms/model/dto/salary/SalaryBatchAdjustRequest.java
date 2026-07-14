package com.limou.hrms.model.dto.salary;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 薪资明细手动调整请求
 */
@Data
public class SalaryBatchAdjustRequest {

    /** 员工ID */
    private Long employeeId;

    /** 调整金额（正数=补发，负数=扣减） */
    private BigDecimal manualAdjust;

    /** 调整原因 */
    private String adjustReason;
}
