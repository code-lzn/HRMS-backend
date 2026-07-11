package com.limou.hrms.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 职位VO
 */
@Data
@ApiModel("职位视图")
public class PositionVO implements Serializable {

    @ApiModelProperty("职位ID")
    private Long id;

    @ApiModelProperty("职位名称")
    private String name;

    @ApiModelProperty("职位序列：1=M 2=P 3=S")
    private Integer sequence;

    @ApiModelProperty("序列名称")
    private String sequenceName;

    @ApiModelProperty("所属部门ID")
    private Long departmentId;

    @ApiModelProperty("所属部门名称")
    private String departmentName;

    @ApiModelProperty("职级下限")
    private String levelMin;

    @ApiModelProperty("职级上限")
    private String levelMax;

    @ApiModelProperty("职级范围，如P1-P10")
    private String levelRange;

    @ApiModelProperty("默认试用期月数")
    private Integer defaultProbationMonths;

    @ApiModelProperty("职位描述")
    private String description;

    @ApiModelProperty("创建时间")
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
