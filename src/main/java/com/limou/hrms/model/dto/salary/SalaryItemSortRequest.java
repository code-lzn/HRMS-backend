package com.limou.hrms.model.dto.salary;

import java.util.List;
import lombok.Data;

/**
 * 工资项目排序请求
 */
@Data
public class SalaryItemSortRequest {

    /**
     * 按顺序排列的项目ID列表
     */
    private List<Long> item_ids;
}
