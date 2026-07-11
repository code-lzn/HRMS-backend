package com.limou.hrms.model.dto.position;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 职位更新请求
 */
@Data
@ApiModel("职位更新请求")
public class PositionUpdateRequest implements Serializable {

    @ApiModelProperty(value = "职位名称", required = true, example = "Java开发工程师")
    private String name;

    @ApiModelProperty(value = "职位序列：1=M管理 2=P专业 3=S支持", required = true, example = "2")
    private Integer sequence;

    @ApiModelProperty(value = "所属部门ID", example = "10")
    private Long departmentId;

    @ApiModelProperty(value = "职级下限", required = true, example = "P1")
    private String levelMin;

    @ApiModelProperty(value = "职级上限", required = true, example = "P10")
    private String levelMax;

    @ApiModelProperty(value = "默认试用期月数", required = true, example = "3")
    private Integer defaultProbationMonths;

    @ApiModelProperty(value = "职位描述", example = "负责后端服务开发")
    private String description;

    private static final long serialVersionUID = 1L;
}
