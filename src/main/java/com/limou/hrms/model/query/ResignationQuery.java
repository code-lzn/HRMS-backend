package com.limou.hrms.model.query;

import com.limou.hrms.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ResignationQuery extends PageRequest {
    private Integer status;
    private Long employeeId;
    private String keyword;
}
