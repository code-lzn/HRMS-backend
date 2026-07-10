package com.limou.hrms.model.dto.salary;

import com.limou.hrms.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 薪资账套查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SalaryAccountQueryRequest extends PageRequest {

    /**
     * 账套名称（模糊搜索）
     */
    private String name;

    /**
     * 适用范围类型
     */
    private Integer scope_type;
}
