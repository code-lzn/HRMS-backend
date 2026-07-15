package com.limou.hrms.model.dto.salary;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import lombok.Data;

/**
 * 审批驳回请求
 */
@ApiModel("审批驳回请求")
@Data
public class SalaryBatchRejectRequest implements Serializable {

    @ApiModelProperty("驳回原因")
    private String reason;

    private static final long serialVersionUID = 1L;
}
