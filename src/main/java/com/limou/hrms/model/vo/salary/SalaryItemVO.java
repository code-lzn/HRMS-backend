package com.limou.hrms.model.vo.salary;

import lombok.Data;

/**
 * 工资项目VO
 */
@Data
public class SalaryItemVO {

    private Long id;

    private Long account_id;

    private String name;

    private Integer item_type;

    private String item_type_label;

    private String formula;

    private Integer sort_order;

    private Integer is_taxable;
}
