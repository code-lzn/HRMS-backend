package com.limou.hrms.model.dto.resignation;

import com.limou.hrms.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 离职申请查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ResignationApplicationQueryRequest extends PageRequest implements Serializable {

    private Long id;
    private Long employeeId;
    private Integer status;
    private Integer leaveType;

    private static final long serialVersionUID = 1L;
}
