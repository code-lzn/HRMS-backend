package com.limou.hrms.model.dto.salary;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import lombok.Data;

/**
 * 薪资账套新增请求
 */
@ApiModel("SalaryAccountAddRequest")
@Data
public class SalaryAccountAddRequest implements Serializable {

    @ApiModelProperty("账套名称")
    private String name;

    @ApiModelProperty("适用范围类型：1=部门, 2=职位, 3=职级")
    private Integer scopeType;

    @ApiModelProperty("适用范围 ID 列表")
    private List<Long> scopeIds;

    @ApiModelProperty("生效日期")
    private Date effectiveDate;

    @ApiModelProperty("工资项目列表（可选，创建时同步添加）")
    private List<SalaryItemAddRequest> items;

    private static final long serialVersionUID = 1L;
}
