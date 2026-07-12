package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 薪资批次表
 * @TableName sal_batch
 */
@TableName(value = "sal_batch")
@Data
public class SalaryBatch implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 批次号 */
    private String batchNo;

    /** 薪资月份: YYYY-MM */
    private String salaryMonth;

    /** 状态: DRAFT=草稿, CALCULATING=计算中, PENDING_CONFIRM=待确认, APPROVING=审批中, APPROVED=已通过, PAID=已发放, REJECTED=已驳回 */
    private String status;

    /** 核算员工总数 */
    private Integer totalEmployeeCount;

    /** 应发工资总额 */
    private BigDecimal totalGross;

    /** 扣除总额 */
    private BigDecimal totalDeduction;

    /** 实发工资总额 */
    private BigDecimal totalNet;

    /** 创建人ID */
    private Long createdBy;

    /** 审批人ID */
    private Long approvedBy;

    /** 实际发放时间 */
    private Date paidAt;

    private Date createdAt;

    private Date updatedAt;

    private static final long serialVersionUID = 1L;
}
