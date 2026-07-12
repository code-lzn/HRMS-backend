package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 审批委托视图
 */
@Data
public class ApprovalDelegationVO implements Serializable {

    private Long id;

    /** 委托人姓名 */
    private String delegatorName;

    /** 被委托人姓名 */
    private String delegateName;

    /** 委托业务类型 */
    private String businessTypes;

    /** 委托开始日期 */
    private Date startDate;

    /** 委托结束日期 */
    private Date endDate;

    /** 状态: 1=有效, 0=已取消 */
    private Integer status;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}
