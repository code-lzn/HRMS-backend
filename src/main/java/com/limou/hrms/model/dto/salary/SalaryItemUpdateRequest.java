package com.limou.hrms.model.dto.salary;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import lombok.Data;

/**
 * 工资项目编辑请求
 */
@ApiModel("SalaryItemUpdateRequest")
@Data
public class SalaryItemUpdateRequest implements Serializable {

    @ApiModelProperty("项目 ID")
    private Long id;

    @ApiModelProperty("项目名称")
    private String name;

    @ApiModelProperty("项目类型")
    private Integer itemType;

    @ApiModelProperty("计算公式")
    private String formula;

    @ApiModelProperty("排序号")
    private Integer sortOrder;

    @ApiModelProperty("是否计入应纳税所得额")
    private Integer isTaxable;

    private static final long serialVersionUID = 1L;
}
