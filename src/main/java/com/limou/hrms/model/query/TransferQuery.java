package com.limou.hrms.model.query;

import com.limou.hrms.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 调岗申请分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TransferQuery extends PageRequest {

    private Integer status;
    private Long employeeId;
    private String keyword;
}
