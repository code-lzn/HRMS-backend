package com.limou.hrms.model.dto.position;

import lombok.Data;

import java.io.Serializable;

/**
 * 职位查询请求
 */
@Data
public class PositionQueryRequest implements Serializable {

    /** 职位序列过滤：1=M 2=P 3=S */
    private Integer sequence;

    /** 部门ID过滤 */
    private Long departmentId;

    private static final long serialVersionUID = 1L;
}
