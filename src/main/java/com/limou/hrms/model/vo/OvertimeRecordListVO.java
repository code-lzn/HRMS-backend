package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class OvertimeRecordListVO implements Serializable {

    private Long id;

    private Long employeeId;

    private String employeeName;

    private String departmentName;

    private LocalDate overtimeDate;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer isUsed;

    private String isUsedDesc;

    private LocalDate expireDate;

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