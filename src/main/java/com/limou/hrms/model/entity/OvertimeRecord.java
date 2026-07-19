package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@TableName(value = "overtime_record")
@Data
public class OvertimeRecord implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long employeeId;

    private Long userId;

    private Date overtimeDate;

    private Date startTime;

    private Date endTime;

    private BigDecimal overtimeHours;

    private Integer overtimeType;

    private String reason;

    private Integer status;

    private Long approverId;

    private Date approveTime;

    private String approveComment;

    private Date createTime;

    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
