package com.limou.hrms.model.dto.salary;

import com.limou.hrms.common.PageRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 薪资明细查询请求
 */
@ApiModel("SalaryDetailQueryRequest")
@EqualsAndHashCode(callSuper = true)
@Data
public class SalaryDetailQueryRequest extends PageRequest {

    @ApiModelProperty("批次 ID")
    private Long batchId;

    @ApiModelProperty("员工 ID")
    private Long employeeId;

    @ApiModelProperty("异常级别")
    private Integer isAbnormal;

    private static final long serialVersionUID = 1L;
}
