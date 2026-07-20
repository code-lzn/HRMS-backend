package com.limou.hrms.model.dto.profile;

import lombok.Data;

import java.io.Serializable;

/**
 * 我的请假列表查询参数
 */
@Data
public class LeaveQueryDTO implements Serializable {

    /**
     * 审批状态：1=草稿 2=审批中 3=已通过 4=已拒绝 5=已取消
     */
    private Integer status;

    /**
     * 页码，默认1
     */
    private Integer page = 1;

    /**
     * 每页条数，默认20
     */
    private Integer size = 20;

    private static final long serialVersionUID = 1L;
}
