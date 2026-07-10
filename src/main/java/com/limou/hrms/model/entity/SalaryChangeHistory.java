package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 调薪历史
 */
@TableName(value = "salary_change_history")
@Data
public class SalaryChangeHistory implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long employee_id;

    private Integer change_type;

    private String old_value;

    private String new_value;

    private Date effective_date;

    private Long operator_id;

    private String remark;

    private Date create_time;

    private static final long serialVersionUID = 1L;
}
