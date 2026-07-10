package com.limou.hrms.model.dto.position;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 职位查询请求
 */
@Data
@ApiModel("职位查询请求")
public class PositionQueryRequest implements Serializable {

    @ApiModelProperty(value = "职位序列过滤：1=M 2=P 3=S", example = "2")
    private Integer sequence;

    @ApiModelProperty(value = "部门ID过滤", example = "10")
    private Long departmentId;

    private static final long serialVersionUID = 1L;
}
