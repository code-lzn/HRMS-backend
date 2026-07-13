package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 员工变更日志 VO
 */
@Data
public class EmployeeChangeLogVO implements Serializable {

    private Long id;
    private Long employeeId;
    private String fieldName;
    private String fieldDesc;
    private String oldValue;
    private String newValue;
    private String changeType;
    private String changeTypeDesc;
    private String operatorName;
    private String remark;
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
