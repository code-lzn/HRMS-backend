package com.limou.hrms.model.dto.transfer;

import com.limou.hrms.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 调岗申请查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TransferApplicationQueryRequest extends PageRequest implements Serializable {

    private Long id;
    private Long employeeId;
    private Integer status;

    private static final long serialVersionUID = 1L;
}
