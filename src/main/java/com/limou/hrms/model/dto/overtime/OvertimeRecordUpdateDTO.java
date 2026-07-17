package com.limou.hrms.model.dto.overtime;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 编辑加班记录 DTO（部分更新）
 */
@Data
public class OvertimeRecordUpdateDTO implements Serializable {

    private LocalDate overtimeDate;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /**
     * 加班小时数，传则同步调整调休余额（差额 delta）
     */
    private BigDecimal hours;

    private static final long serialVersionUID = 1L;
}