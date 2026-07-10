package com.limou.hrms.model.vo.salary;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 社保公积金对比VO（AntV分组柱状图）
 */
@Data
public class SocialComparisonVO {

    private String department_name;

    private BigDecimal total_social_security;

    private BigDecimal total_housing_fund;
}
