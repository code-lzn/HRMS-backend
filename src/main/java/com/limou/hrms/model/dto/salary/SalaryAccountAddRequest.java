package com.limou.hrms.model.dto.salary;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Date;
import java.util.List;
import lombok.Data;

/**
 * 薪资账套新增请求
 */
@ApiModel("新建账套请求")
@Data
public class SalaryAccountAddRequest {

    @ApiModelProperty("账套名称")
    private String name;

    @ApiModelProperty("适用范围类型：1=部门 2=职位 3=职级")
    private Integer scope_type;

    @ApiModelProperty("适用范围ID集合")
    private List<Long> scope_ids;

    @ApiModelProperty("生效日期")
    private Date effective_date;

    @ApiModelProperty("工资项目列表（可选，创建时同步添加）")
    private List<SalaryItemAddRequest> items;
}
