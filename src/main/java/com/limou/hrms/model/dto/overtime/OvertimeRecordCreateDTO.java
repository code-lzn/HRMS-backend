package com.limou.hrms.model.dto.overtime;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 创建加班记录 DTO
 */
@Data
public class OvertimeRecordCreateDTO implements Serializable {

    /**
     * 员工ID
     */
    @NotNull(message = "员工ID不能为空")
    private Long employeeId;

    /**
     * 加班日期
     */
    @NotNull(message = "加班日期不能为空")
    private LocalDate overtimeDate;

    /**
     * 加班开始时间
     */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /**
     * 加班结束时间
     */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    /**
     * 加班小时数
     */
    @NotNull(message = "加班小时数不能为空")
    private BigDecimal hours;

    private static final long serialVersionUID = 1L;
}