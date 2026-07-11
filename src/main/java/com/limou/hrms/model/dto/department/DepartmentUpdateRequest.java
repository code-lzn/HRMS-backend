package com.limou.hrms.model.dto.department;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 部门更新请求
 */
@Data
@ApiModel("部门更新请求")
public class DepartmentUpdateRequest implements Serializable {

    @ApiModelProperty(value = "部门名称", required = true, example = "技术部")
    private String name;

    @ApiModelProperty(value = "部门负责人ID", example = "100")
    private Long managerId;

    @ApiModelProperty(value = "排序序号", required = true, example = "0")
    private Integer sortOrder;

    @ApiModelProperty(value = "部门描述", example = "负责技术研发")
    private String description;

    private static final long serialVersionUID = 1L;
}
