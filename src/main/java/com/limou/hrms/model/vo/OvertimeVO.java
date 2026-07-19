package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class OvertimeVO implements Serializable {

    private Long id;

    private Long employeeId;

    private String employeeName;

    private Date overtimeDate;

    private Date startTime;

    private Date endTime;

    private BigDecimal overtimeHours;

    private Integer overtimeType;

    private String overtimeTypeText;

    private String reason;

    private Integer status;

    private String statusText;

    private Long approverId;

    private Date approveTime;

    private String approveComment;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}
