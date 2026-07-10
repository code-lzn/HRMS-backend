package com.limou.hrms.model.dto.salary;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 薪资手动调整请求
 */
@ApiModel("手动调整薪资请求")
@Data
public class SalaryAdjustRequest {

    @ApiModelProperty("薪资明细ID")
    private Long detail_id;

    @ApiModelProperty("调整金额（正数为补发，负数为扣减）")
    private BigDecimal adjustment_amount;

    @ApiModelProperty("调整原因")
    private String adjustment_reason;
}
