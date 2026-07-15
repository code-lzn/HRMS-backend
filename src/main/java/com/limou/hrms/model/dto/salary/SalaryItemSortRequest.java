package com.limou.hrms.model.dto.salary;

import lombok.Data;

import java.util.List;

/**
 * 工资项目排序请求
 */
@Data
public class SalaryItemSortRequest {

    /** 排序后的项目ID列表 */
    private List<Long> itemIds;
}
