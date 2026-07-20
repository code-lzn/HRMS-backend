package com.limou.hrms.model.dto.salary;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import lombok.Data;

/**
 * 薪资核算批次创建请求
 */
@ApiModel("SalaryBatchCreateRequest")
@Data
public class SalaryBatchCreateRequest implements Serializable {

    @ApiModelProperty("薪资月份，格式 yyyy-MM")
    private String salaryMonth;

    private static final long serialVersionUID = 1L;
}
