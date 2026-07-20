package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 薪资核算批次
 */
@TableName(value = "salary_batch")
@Data
public class SalaryBatch implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 批次号，如"SAL202607"
     */
    private String batchNo;

    /**
     * 薪资月份 yyyy-MM
     */
    private String salaryMonth;

    /**
     * 状态：0=草稿, 1=计算中, 2=待确认, 3=审批中, 4=已通过, 5=已发放, 6=已驳回
     */
    private Integer status;

    /**
     * 参与核算员工数
     */
    private Integer totalEmployees;

    /**
     * 应发工资合计
     */
    private BigDecimal totalGrossPay;

    /**
     * 实发工资合计
     */
    private BigDecimal totalNetPay;

    /**
     * 个税合计
     */
    private BigDecimal totalTax;

    /**
     * 创建人 ID（HR）→ user.id
     */
    private Long createBy;
    private Long approvalInstanceId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;


    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
