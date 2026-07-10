package com.limou.hrms.model.dto.salary;

import com.limou.hrms.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 薪资批次查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SalaryBatchQueryRequest extends PageRequest {

    /**
     * 核算月份
     */
    private String salary_month;

    /**
     * 批次状态
     */
    private Integer status;
}
