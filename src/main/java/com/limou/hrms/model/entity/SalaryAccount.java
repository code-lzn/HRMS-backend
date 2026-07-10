package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 薪资账套
 */
@TableName(value = "salary_account")
@Data
public class SalaryAccount implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;

    private Integer scope_type;

    private String scope_ids;

    private Date effective_date;

    @TableLogic(value = "0", delval = "1")
    private Integer is_deleted;

    private Date create_time;

    private Date update_time;

    private static final long serialVersionUID = 1L;
}
