package com.limou.hrms.model.dto.department;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 部门合并请求
 */
@Data
@ApiModel("部门合并请求")
public class DepartmentMergeRequest implements Serializable {

    @ApiModelProperty(value = "源部门ID（被合并）", required = true, example = "10")
    private Long sourceDeptId;

    @ApiModelProperty(value = "目标部门ID（保留）", required = true, example = "20")
    private Long targetDeptId;

    private static final long serialVersionUID = 1L;
}
