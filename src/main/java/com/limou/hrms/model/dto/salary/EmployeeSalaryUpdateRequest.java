package com.limou.hrms.model.dto.salary;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 员工薪资档案更新请求
 */
@Data
public class EmployeeSalaryUpdateRequest {

    /** 适用账套ID */
    private Long accountSetId;

    /** 基本工资 */
    private BigDecimal baseSalary;

    /** 岗位津贴基数 */
    private BigDecimal allowanceBase;

    /** 社保缴纳基数 */
    private BigDecimal socialInsuranceBase;

    /** 公积金缴纳基数 */
    private BigDecimal housingFundBase;

    /** 绩效奖金基数 */
    private BigDecimal performanceBase;

    /** 试用期薪资比例 */
    private BigDecimal probationSalaryRatio;

    /** 生效日期 */
    private Date effectiveDate;

    /** 备注 */
    private String remark;
}
