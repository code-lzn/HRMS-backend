package com.limou.hrms.model.dto.overtime;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    //添加时间格式注解
    private LocalDateTime endTime;

    private static final long serialVersionUID = 1L;
}