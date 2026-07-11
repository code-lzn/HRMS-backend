package com.limou.hrms.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 部门合并结果VO
 */
@Data
@ApiModel("部门合并结果")
public class DepartmentMergeResultVO implements Serializable {

    @ApiModelProperty("转移员工数")
    private Integer transferredEmployees;

    @ApiModelProperty("转移子部门数")
    private Integer transferredChildDepts;

    private static final long serialVersionUID = 1L;
}
