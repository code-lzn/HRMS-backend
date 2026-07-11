package com.limou.hrms.model.dto.resignation;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 离职申请创建请求
 */
@Data
public class ResignationApplicationAddRequest implements Serializable {

    private Long employeeId;
    private Date leaveDate;
    private String leaveReason;
    private String leaveReasonDetail;
    private Integer leaveType;
    private Long handoverPersonId;

    private static final long serialVersionUID = 1L;
}
