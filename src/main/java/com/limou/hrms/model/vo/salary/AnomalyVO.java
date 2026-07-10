package com.limou.hrms.model.vo.salary;

import lombok.Data;

/**
 * 异常项VO
 */
@Data
public class AnomalyVO {

    private Long detail_id;

    private Long employee_id;

    private String employee_no;

    private String employee_name;

    private String department_name;

    private Integer is_abnormal;

    private String abnormal_reason;

    private String abnormal_level_label;
}
