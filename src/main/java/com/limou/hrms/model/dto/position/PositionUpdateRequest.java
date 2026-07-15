package com.limou.hrms.model.dto.position;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新职位请求
 */
@Data
public class PositionUpdateRequest implements Serializable {

    /**
     * 职位名称
     */
    private String name;

    /**
     * 职位序列
     */
    private Integer sequence;

    /**
     * 所属部门ID，传null表示清空（变为全公司通用）
     */
    private Long departmentId;

    /**
     * 职级下限
     */
    private Integer levelMin;

    /**
     * 职级上限
     */
    private Integer levelMax;

    /**
     * 默认试用期（月）
     */
    private Integer defaultProbationMonths;

    /**
     * 职位描述，传null表示清空
     */
    private String description;

    private static final long serialVersionUID = 1L;
}
