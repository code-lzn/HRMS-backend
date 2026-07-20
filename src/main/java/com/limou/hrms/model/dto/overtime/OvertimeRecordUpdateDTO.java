package com.limou.hrms.model.dto.overtime;

import lombok.Data;

import java.io.Serializable;
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

    private static final long serialVersionUID = 1L;
}