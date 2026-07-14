package com.limou.hrms.model.dto.salary;

import lombok.Data;

/**
 * 审批驳回请求
 */
@Data
public class SalaryBatchRejectRequest {

    /** 驳回原因 */
    private String reason;
}
