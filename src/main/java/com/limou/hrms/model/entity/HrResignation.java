package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@TableName(value = "emp_resign")
@Data
public class HrResignation implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String businessNo;

    private Long flowId;

    private Long recordId;

    private Long employeeId;

    private Long approverId;

    private Date applyDate;

    private Date lastWorkDate;

    private Integer resignType;

    private String resignReason;

    private String resignReasonType;

    private Long handoverPersonId;

    private Integer handoverStatus;

    private BigDecimal settleSalary;

    private Date settleDate;

    private String status;

    private Long operatorId;

    private String remark;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDeleted;

    private static final long serialVersionUID = 1L;
}
