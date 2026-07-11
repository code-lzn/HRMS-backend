package com.limou.hrms.model.dto.position;

import lombok.Data;

import java.io.Serializable;

/**
 * 职位更新请求
 */
@Data
public class PositionUpdateRequest implements Serializable {

    /** 职位ID */
    private Long id;

    /** 职位名称 */
    private String name;

    /** 职位序列：1=M管理 2=P专业 3=S支持 */
    private Integer sequence;

    /** 所属部门ID */
    private Long departmentId;

    /** 职级下限 */
    private String levelMin;

    /** 职级上限 */
    private String levelMax;

    /** 默认试用期月数 */
    private Integer defaultProbationMonths;

    /** 职位描述 */
    private String description;

    private static final long serialVersionUID = 1L;
}
