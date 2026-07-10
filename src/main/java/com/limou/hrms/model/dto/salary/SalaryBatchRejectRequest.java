package com.limou.hrms.model.dto.salary;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 薪资审批驳回请求
 */
@ApiModel("驳回请求")
@Data
public class SalaryBatchRejectRequest {

    @ApiModelProperty("驳回原因")
    private String reason;
}
