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
public class PositionVO implements Serializable {

    /** 职位ID */
    private Long id;

    /** 职位名称 */
    private String name;

    /** 职位序列：1=M 2=P 3=S */
    private Integer sequence;

    /** 序列名称 */
    private String sequenceName;

    /** 所属部门ID */
    private Long departmentId;

    /** 所属部门名称 */
    private String departmentName;

    /** 职级下限 */
    private String levelMin;

    /** 职级上限 */
    private String levelMax;

    /** 职级范围，如P1-P10 */
    private String levelRange;

    /** 默认试用期月数 */
    private Integer defaultProbationMonths;

    /** 职位描述 */
    private String description;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}
