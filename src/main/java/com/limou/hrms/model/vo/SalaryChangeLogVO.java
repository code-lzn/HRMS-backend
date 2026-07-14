package com.limou.hrms.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * 调薪历史视图
 */
@Data
public class SalaryChangeLogVO {

    private Long id;
    private Long employeeId;
    private Integer changeType;
    private String changeTypeText;
    private String oldValue;
    private String newValue;
    private Date effectiveDate;
    private Long operatorId;
    private String operatorName;
    private String remark;
    private Date createTime;
}
