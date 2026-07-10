package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 薪资核算批次
 */
@TableName(value = "salary_batch")
@Data
public class SalaryBatch implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String batch_no;

    private String salary_month;

    private Integer status;

    private Integer total_employees;

    private BigDecimal total_gross_pay;

    private BigDecimal total_net_pay;

    private BigDecimal total_tax;

    private Long create_by;

    private Date create_time;

    private Date update_time;

    private static final long serialVersionUID = 1L;
}
