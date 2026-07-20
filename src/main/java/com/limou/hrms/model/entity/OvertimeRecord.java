package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("overtime_record")
@Data
public class OvertimeRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long employeeId;

    private LocalDate overtimeDate;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer isUsed;

    private LocalDate expireDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
