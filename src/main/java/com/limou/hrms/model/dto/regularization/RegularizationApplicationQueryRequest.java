package com.limou.hrms.model.dto.regularization;

import com.limou.hrms.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 转正申请查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RegularizationApplicationQueryRequest extends PageRequest implements Serializable {

    private Long id;
    private Long employeeId;
    private Integer status;
    private Integer result;

    private static final long serialVersionUID = 1L;
}
