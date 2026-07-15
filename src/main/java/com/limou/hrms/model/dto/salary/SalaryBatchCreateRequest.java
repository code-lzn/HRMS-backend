package com.limou.hrms.model.dto.salary;

import lombok.Data;

/**
 * 薪资核算批次创建请求
 */
@Data
public class SalaryBatchCreateRequest {

    /** 核算月份，格式 yyyy-MM */
    private String salaryMonth;
}
