package com.limou.hrms.model.dto.salary;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 工资条手动调整请求
 */
@ApiModel("手动调整薪资请求")
@Data
public class SalaryDetailAdjustRequest implements Serializable {

    @ApiModelProperty("调整金额（正=补发, 负=扣回）")
    private BigDecimal adjustment;

    @ApiModelProperty("调整原因")
    private String reason;

    private static final long serialVersionUID = 1L;
}
