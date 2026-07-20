package com.limou.hrms.model.dto.overtime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 创建加班记录 DTO
 */
@Data
public class OvertimeRecordCreateDTO implements Serializable {

    @NotNull(message = "员工ID不能为空")
    private Long employeeId;

    @NotNull(message = "加班日期不能为空")
    private LocalDate overtimeDate;

    @NotNull(message = "开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    private static final long serialVersionUID = 1L;
}