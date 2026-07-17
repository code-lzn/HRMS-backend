package com.limou.hrms.model.dto.probation;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 处理转正结果请求体（拒绝后HR决定延期/辞退）
 */
@Data
public class ProbationHandleResultDTO {

    @NotNull(message = "处理结果不能为空")
    private Integer result;

    /** 延长后的试用期结束日期（result=EXTEND时必填） */
    private LocalDate extendedEndDate;
}
