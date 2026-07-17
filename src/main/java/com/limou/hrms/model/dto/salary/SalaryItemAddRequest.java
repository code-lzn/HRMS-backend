package com.limou.hrms.model.dto.salary;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import lombok.Data;

/**
 * 工资项目新增请求
 */
@ApiModel("SalaryItemAddRequest")
@Data
public class SalaryItemAddRequest implements Serializable {

    @ApiModelProperty("项目名称")
    private String name;

    @ApiModelProperty("项目类型：1=固定收入, 2=变动收入, 3=考勤扣款, 4=社保扣除, 5=公积金扣除, 6=个税")
    private Integer itemType;

    @ApiModelProperty("计算公式")
    private String formula;

    @ApiModelProperty("排序号")
    private Integer sortOrder;

    @ApiModelProperty("是否计入应纳税所得额：1=是, 0=否")
    private Integer isTaxable;

    private static final long serialVersionUID = 1L;
}
