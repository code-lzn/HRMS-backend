package com.limou.hrms.model.dto.salary;

import com.limou.hrms.common.PageRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 薪资批次查询请求
 */
@ApiModel("SalaryBatchQueryRequest")
@EqualsAndHashCode(callSuper = true)
@Data
public class SalaryBatchQueryRequest extends PageRequest {

    @ApiModelProperty("薪资月份")
    private String salaryMonth;

    @ApiModelProperty("批次状态")
    private Integer status;

    private static final long serialVersionUID = 1L;
}
