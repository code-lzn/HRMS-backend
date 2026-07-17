package com.limou.hrms.model.dto.salary;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import lombok.Data;

/**
 * 员工薪资档案更新请求
 */
@ApiModel("EmployeeSalaryUpdateRequest")
@Data
public class EmployeeSalaryUpdateRequest implements Serializable {

    @ApiModelProperty("薪资账套 ID")
    private Long accountId;

    @ApiModelProperty("月基本工资")
    private BigDecimal baseSalary;

    @ApiModelProperty("津贴补贴基数")
    private BigDecimal allowanceBase;

    @ApiModelProperty("社保缴费基数")
    private BigDecimal socialSecurityBase;

    @ApiModelProperty("公积金缴费基数")
    private BigDecimal housingFundBase;

    @ApiModelProperty("绩效工资基数")
    private BigDecimal performanceBase;

    @ApiModelProperty("生效日期")
    private LocalDate effectiveDate;

    @ApiModelProperty("变更备注")
    private String remark;

    private static final long serialVersionUID = 1L;
}
