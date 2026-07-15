package com.limou.hrms.model.vo.salary;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 调薪历史 VO
 */
@Data
public class SalaryChangeHistoryVO implements Serializable {

    private Long id;

    private Long employeeId;

    private Integer changeType;

    private String changeTypeLabel;

    private String oldValue;

    private String newValue;

    private Date effectiveDate;

    private Long operatorId;

    private String remark;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}
