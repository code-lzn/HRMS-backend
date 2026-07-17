package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
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

    private BigDecimal hours;

    private Integer isUsed;

    private String isUsedDesc;

    private LocalDate expireDate;

    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;
}