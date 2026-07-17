package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("supplement_card_request")
@Data
public class SupplementCardRequest implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long employeeId;

    private LocalDate attendanceDate;

    private Integer cardType;

    private String reason;

    private Integer status;

    private Long approvalInstanceId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
