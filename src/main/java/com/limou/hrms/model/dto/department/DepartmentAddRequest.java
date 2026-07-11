package com.limou.hrms.model.dto.department;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 部门新增请求
 */
@Data
@ApiModel("部门新增请求")
public class DepartmentAddRequest implements Serializable {

    @ApiModelProperty(value = "部门名称", required = true, example = "技术部")
    private String name;

    @ApiModelProperty(value = "部门编码（2位）", required = true, example = "01")
    private String code;

    @ApiModelProperty(value = "上级部门ID，空表示根部门", example = "1")
    private Long parentId;

    @ApiModelProperty(value = "部门负责人ID", example = "100")
    private Long managerId;

    @ApiModelProperty(value = "排序序号，越小越靠前", required = true, example = "0")
    private Integer sortOrder;

    @ApiModelProperty(value = "部门描述", example = "负责技术研发")
    private String description;

    private static final long serialVersionUID = 1L;
}
