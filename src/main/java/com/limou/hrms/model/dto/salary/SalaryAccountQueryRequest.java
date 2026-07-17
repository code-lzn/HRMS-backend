package com.limou.hrms.model.dto.salary;

import com.limou.hrms.common.PageRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 薪资账套查询请求
 */
@ApiModel("SalaryAccountQueryRequest")
@EqualsAndHashCode(callSuper = true)
@Data
public class SalaryAccountQueryRequest extends PageRequest {

    @ApiModelProperty("账套名称（模糊搜索）")
    private String name;

    @ApiModelProperty("适用范围类型：1=部门, 2=职位, 3=职级")
    private Integer scopeType;

    private static final long serialVersionUID = 1L;
}
