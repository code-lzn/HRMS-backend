package com.limou.hrms.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 薪资趋势 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryTrendVO implements Serializable {

    /**
     * 月份列表，如 ["2024-02", "2024-03", ...]
     */
    private List<String> months;

    /**
     * 对应月份实发工资
     */
    private List<BigDecimal> netSalaries;

    private static final long serialVersionUID = 1L;
}
