package com.limou.hrms.model.vo;

import lombok.Data;

/**
 * 工资项目视图
 */
@Data
public class SalaryItemVO {

    /** 主键ID */
    private Long id;
    /** 账套ID */
    private Long accountId;
    /** 项目名称 */
    private String name;
    /** 项目类型 */
    private Integer itemType;
    /** 项目类型文本 */
    private String itemTypeText;
    /** 计算公式 */
    private String formula;
    /** 排序序号 */
    private Integer sortOrder;
    /** 是否计税 */
    private Integer isTaxable;
}
