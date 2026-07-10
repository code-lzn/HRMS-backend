package com.limou.hrms.model.vo.salary;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 薪资成本趋势VO（AntV折线图）
 */
@Data
public class CostTrendVO {

    private String month;

    private BigDecimal total_cost;

    private BigDecimal yoy_change_rate;
}
