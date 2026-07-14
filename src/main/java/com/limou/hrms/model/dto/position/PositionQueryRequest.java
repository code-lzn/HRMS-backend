package com.limou.hrms.model.dto.position;

import com.limou.hrms.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 职位列表查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PositionQueryRequest extends PageRequest implements Serializable {

    /**
     * 职位名称模糊搜索
     */
    private String keyword;

    /**
     * 职位序列筛选：1=M 2=P 3=S
     */
    private Integer sequence;

    /**
     * 所属部门筛选
     */
    private Long departmentId;

    private static final long serialVersionUID = 1L;
}
