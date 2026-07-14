package com.limou.hrms.model.dto.position;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 创建职位请求
 */
@Data
public class PositionCreateRequest implements Serializable {

    /**
     * 职位名称
     */
    @NotBlank(message = "职位名称不能为空")
    private String name;

    /**
     * 职位序列：1=M 2=P 3=S
     */
    @NotNull(message = "职位序列不能为空")
    private Integer sequence;

    /**
     * 所属部门ID，不传表示全公司通用
     */
    private Long departmentId;

    /**
     * 职级下限
     */
    @NotNull(message = "职级下限不能为空")
    private Integer levelMin;

    /**
     * 职级上限
     */
    @NotNull(message = "职级上限不能为空")
    private Integer levelMax;

    /**
     * 默认试用期（月）
     */
    @NotNull(message = "默认试用期不能为空")
    private Integer defaultProbationMonths;

    /**
     * 职位描述
     */
    private String description;

    private static final long serialVersionUID = 1L;
}
