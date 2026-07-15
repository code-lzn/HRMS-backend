package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 职位列表/详情 VO
 */
@Data
public class PositionVO implements Serializable {

    private Long id;

    private String name;

    private Integer sequence;

    /**
     * 序列描述（如"专业序列P"）
     */
    private String sequenceDesc;

    private Long departmentId;

    /**
     * 部门名称，null时显示"全公司通用"
     */
    private String departmentName;

    /**
     * 职级范围（如 P1-P10）
     */
    private String levelRange;

    private Integer levelMin;

    private Integer levelMax;

    private Integer defaultProbationMonths;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}
