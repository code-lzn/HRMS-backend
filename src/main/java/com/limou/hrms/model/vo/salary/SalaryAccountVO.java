package com.limou.hrms.model.vo.salary;

import java.util.Date;
import java.util.List;
import lombok.Data;

/**
 * 薪资账套详情VO（含工资项目列表）
 */
@Data
public class SalaryAccountVO {

    private Long id;

    private String name;

    private Integer scope_type;

    private String scope_type_label;

    private String scope_ids;

    private Date effective_date;

    private Date create_time;

    private Date update_time;

    /**
     * 工资项目列表
     */
    private List<SalaryItemVO> items;
}
