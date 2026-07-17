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

    private Long id;

    private Long employeeId;

    private LocalDate attendanceDate;

    private Integer cardType;

    private String cardTypeDesc;

    private String reason;

    private Integer status;

    private String statusDesc;

    /**
     * 当月已申请次数
     */
    private int monthlyCount;

    /**
     * 每月上限
     */
    private int monthlyLimit = 2;

    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;
}
