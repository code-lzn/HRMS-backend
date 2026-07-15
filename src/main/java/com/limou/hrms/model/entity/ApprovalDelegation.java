package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 审批委托
 * @TableName approval_delegation
 */
@TableName(value = "approval_delegation")
@Data
public class ApprovalDelegation implements Serializable {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 委托人ID（employeeId） */
    private Long delegatorId;

    /** 委托人姓名 */
    private String delegatorName;

    /** 被委托人ID（employeeId） */
    private Long delegateId;

    /** 被委托人姓名 */
    private String delegateName;

    /** 委托业务类型（逗号分隔，NULL=全部） */
    private String businessTypes;

    /** 委托开始日期 */
    private Date startDate;

    /** 委托结束日期 */
    private Date endDate;

    /** 状态: 1=有效, 0=已取消 */
    private Integer status;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
