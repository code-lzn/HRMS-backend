package com.limou.hrms.model.vo.salary;

import java.util.Date;
import lombok.Data;

/**
 * 调薪历史VO
 */
@Data
public class SalaryChangeHistoryVO {

    private Long id;

    private Long employee_id;

    private Integer change_type;

    private String change_type_label;

    private String old_value;

    private String new_value;

    private Date effective_date;

    private Long operator_id;

    private String operator_name;

    private String remark;

    private Date create_time;
}
