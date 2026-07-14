package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@TableName("salary_batch")
@Data
public class SalaryBatch implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String batchNo;
    private String salaryMonth;
    /** 状态：0=草稿 1=计算中 2=待确认 3=审批中 4=已通过 5=已发放 6=已驳回 */
    private Integer status;
    private Integer totalEmployees;
    private BigDecimal totalGrossPay;
    private BigDecimal totalNetPay;
    private BigDecimal totalTax;
    private Long createBy;
    private Long approvalInstanceId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}
