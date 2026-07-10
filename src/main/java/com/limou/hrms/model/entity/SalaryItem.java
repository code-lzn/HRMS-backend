package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 工资项目
 */
@TableName(value = "salary_item")
@Data
public class SalaryItem implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long account_id;

    private String name;

    private Integer item_type;

    private String formula;

    private Integer sort_order;

    private Integer is_taxable;

    private Date create_time;

    private Date update_time;

    private static final long serialVersionUID = 1L;
}
