package com.limou.hrms.model.vo.salary;

import lombok.Data;

/**
 * 薪资变动分布VO（AntV直方图）
 */
@Data
public class VariationDistributionVO {

    private String range_label;

    private Integer range_start;

    private Integer range_end;

    private Integer employee_count;
}
