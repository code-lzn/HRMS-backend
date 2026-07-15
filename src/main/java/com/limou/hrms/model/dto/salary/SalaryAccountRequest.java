package com.limou.hrms.model.dto.salary;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 薪资账套新增/编辑请求
 */
@Data
public class SalaryAccountRequest {

    /** 账套名称 */
    private String name;

    /** 适用范围类型：1=部门 2=职位 3=职级 */
    private Integer scopeType;

    /** 适用范围ID集合(JSON数组) */
    private String scopeIds;

    /** 生效日期 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date effectiveDate;

    /** 工资项目列表 */
    private List<SalaryItemRequest> items;
}
