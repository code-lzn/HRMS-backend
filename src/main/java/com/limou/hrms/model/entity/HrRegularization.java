package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@TableName(value = "emp_probation")
@Data
public class HrRegularization implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String businessNo;

    private Long flowId;

    private Long recordId;

    private Long employeeId;

    private Long approverId;

    private Date originHireDate;

    private Date probationEndDate;

    private Date confirmDate;

    private BigDecimal probationScore;

    private String probationComment;

    private BigDecimal confirmBaseSalary;

    private BigDecimal salaryAdjustment;

    private String adjustRemark;

    private String result;

    private Integer extendedMonths;

    private String status;

    private Long operatorId;

    private String remark;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDeleted;

    private static final long serialVersionUID = 1L;
}
