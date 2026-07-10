package com.limou.hrms.model.dto.salary;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Date;
import java.util.List;
import lombok.Data;

/**
 * 薪资账套编辑请求
 */
@ApiModel("编辑账套请求")
@Data
public class SalaryAccountUpdateRequest {

    @ApiModelProperty("账套ID")
    private Long id;

    @ApiModelProperty("账套名称")
    private String name;

    @ApiModelProperty("适用范围类型：1=部门 2=职位 3=职级")
    private Integer scope_type;

    @ApiModelProperty("适用范围ID集合")
    private List<Long> scope_ids;

    @ApiModelProperty("生效日期")
    private Date effective_date;
}
