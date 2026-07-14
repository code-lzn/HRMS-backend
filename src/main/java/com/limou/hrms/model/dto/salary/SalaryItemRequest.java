package com.limou.hrms.model.dto.salary;

import lombok.Data;

/**
 * 工资项目新增/编辑请求
 */
@Data
public class SalaryItemRequest {

    /** 项目名称 */
    private String name;

    /** 项目类型：1=固定收入 2=变动收入 3=考勤扣款 4=社保扣除 5=公积金扣除 6=个税 */
    private Integer itemType;

    /** 计算公式/规则描述 */
    private String formula;

    /** 排序序号 */
    private Integer sortOrder;

    /** 是否计入个税：0=否 1=是 */
    private Integer isTaxable;
}
