package com.limou.hrms.model.vo;

import lombok.Data;

/**
 * 工资项目视图
 */
@Data
public class SalaryItemVO {

    private Long id;
    private Long accountId;
    private String name;
    private Integer itemType;
    private String itemTypeText;
    private String formula;
    private Integer sortOrder;
    private Integer isTaxable;
}
