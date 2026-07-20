package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
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

    private LocalDate expireDate;

    /**
     * 本次新转入的调休天数
     */
    private BigDecimal compTimeAdded;

    private LocalDateTime createTime;

    /**
     * 加班小时数（由 startTime ~ endTime 自动计算）
     */
    public BigDecimal getHours() {
        if (startTime == null || endTime == null) return BigDecimal.ZERO;
        long minutes = Duration.between(startTime, endTime).toMinutes();
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);
    }

    private static final long serialVersionUID = 1L;
}