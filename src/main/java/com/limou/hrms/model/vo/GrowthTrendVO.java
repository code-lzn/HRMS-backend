package com.limou.hrms.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 数据分析 - 用户增长与收入趋势
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrowthTrendVO implements Serializable {

    /** 日期 */
    private String date;
    /** 用户数 */
    private Long userCount;
    /** 收入 */
    private BigDecimal revenue;

    private static final long serialVersionUID = 1L;
}
