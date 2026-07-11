package com.limou.hrms.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 序列职级对照VO
 */
@Data
@ApiModel("序列职级对照")
public class SequenceLevelVO implements Serializable {

    @ApiModelProperty("序列值：1=M 2=P 3=S")
    private Integer sequence;

    @ApiModelProperty("序列名称")
    private String sequenceName;

    @ApiModelProperty("序列编码：M/P/S")
    private String sequenceCode;

    @ApiModelProperty("职级列表")
    private List<String> levels;

    private static final long serialVersionUID = 1L;
}
