package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 加班记录 VO
 */
@Data
public class OvertimeRecordVO implements Serializable {

    private Long id;

    private Long employeeId;

    private LocalDate overtimeDate;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private BigDecimal hours;

    private LocalDate expireDate;

    /**
     * 本次新转入的调休天数
     */
    private BigDecimal compTimeAdded;

    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;
}