package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 职位列表/详情 VO
 */
@Data
public class PositionVO implements Serializable {

    /** 职位ID */
    private Long id;

    /** 职位名称 */
    private String name;

    /** 职位序列值：1=管理序列M 2=专业序列P 3=支持序列S */
    private Integer sequence;

    /** 序列描述（如"专业序列P"） */
    private String sequenceDesc;

    /** 所属部门ID，null表示全公司通用 */
    private Long departmentId;

    /** 部门名称，null时显示"全公司通用" */
    private String departmentName;

    /** 职级范围（如 P1-P10） */
    private String levelRange;

    /** 职级下限 */
    private Integer levelMin;

    /** 职级上限 */
    private Integer levelMax;

    /** 默认试用期（月） */
    private Integer defaultProbationMonths;

    /** 该职位在职员工数 */
    private Integer employeeCount;

    /** 职位描述 */
    private String description;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}