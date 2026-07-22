package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 补卡申请 VO
 */
@Data
public class SupplementCardVO implements Serializable {

    /** 申请ID */
    private Long id;

    /** 员工ID */
    private Long employeeId;

    /** 补卡日期 */
    private LocalDate attendanceDate;

    /** 补卡类型：1=上班卡 2=下班卡 */
    private Integer cardType;

    /** 补卡类型描述 */
    private String cardTypeDesc;

    /** 补卡原因 */
    private String reason;

    /** 状态：1=草稿 2=审批中 3=已通过 4=已拒绝 */
    private Integer status;

    /** 状态描述 */
    private String statusDesc;

    /** 当月已申请次数 */
    private int monthlyCount;

    /** 每月上限（默认 2） */
    private int monthlyLimit = 2;

    /** 申请时间 */
    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;
}