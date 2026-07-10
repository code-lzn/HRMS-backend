package com.limou.hrms.model.dto.salary;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 薪资核算批次创建请求
 */
@ApiModel("新建核算批次请求")
@Data
public class SalaryBatchCreateRequest {

    @ApiModelProperty("核算月份，格式yyyy-MM，如2024-07")
    private String salary_month;
}
