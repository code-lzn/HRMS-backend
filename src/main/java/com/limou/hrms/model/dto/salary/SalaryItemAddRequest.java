package com.limou.hrms.model.dto.salary;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 工资项目新增请求
 */
@ApiModel("添加工资项目请求")
@Data
public class SalaryItemAddRequest {

    @ApiModelProperty("项目名称")
    private String name;

    @ApiModelProperty("项目类型：1=固定收入 2=变动收入 3=考勤扣款 4=社保扣除 5=公积金扣除 6=个税")
    private Integer item_type;

    @ApiModelProperty("计算公式/规则描述")
    private String formula;

    @ApiModelProperty("排序序号")
    private Integer sort_order;

    @ApiModelProperty("是否计入个税：0=否 1=是")
    private Integer is_taxable;
}
