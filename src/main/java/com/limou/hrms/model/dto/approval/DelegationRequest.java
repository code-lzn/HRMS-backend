package com.limou.hrms.model.dto.approval;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 审批委托请求
 */
@Data
public class DelegationRequest implements Serializable {

    /** 被委托人ID（employeeId） */
    private Long delegateId;

    /** 委托业务类型（逗号分隔，不传=全部） */
    private String businessTypes;

    /** 委托开始日期 */
    private Date startDate;

    /** 委托结束日期 */
    private Date endDate;

    private static final long serialVersionUID = 1L;
}
