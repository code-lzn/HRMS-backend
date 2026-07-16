package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@TableName(value = "emp_transfer")
@Data
public class HrTransfer implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String businessNo;

    private Long flowId;

    private Long recordId;

    private Long employeeId;

    private Long approverId;

    private Long sourceDeptId;

    private Long sourcePositionId;

    private Long targetDeptId;

    private Long targetPositionId;

    private Date transferDate;

    private String transferReason;

    private BigDecimal newBaseSalary;

    private String toRankCode;

    private Long toReporterId;

    private String status;

    private Long operatorId;

    private String remark;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDeleted;

    private static final long serialVersionUID = 1L;
}
