package com.limou.hrms.model.query;

import com.limou.hrms.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批列表查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ApprovalQuery extends PageRequest {
    /** 业务类型筛选，可选 */
    private String bizType;
}
