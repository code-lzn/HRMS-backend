package com.limou.hrms.model.dto.salary;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 员工薪资档案更新请求
 */
@ApiModel("更新薪资档案请求")
@Data
public class EmployeeSalaryUpdateRequest {

    @ApiModelProperty("适用账套ID")
    private Long account_id;

    @ApiModelProperty("基本工资（元）")
    private BigDecimal base_salary;

    @ApiModelProperty("津贴基数（元）")
    private BigDecimal allowance_base;

    @ApiModelProperty("社保基数（元）")
    private BigDecimal social_security_base;

    @ApiModelProperty("公积金基数（元）")
    private BigDecimal housing_fund_base;

    @ApiModelProperty("绩效基数（元）")
    private BigDecimal performance_base;

    @ApiModelProperty("生效日期")
    private Date effective_date;

    @ApiModelProperty("变更备注")
    private String remark;
}
