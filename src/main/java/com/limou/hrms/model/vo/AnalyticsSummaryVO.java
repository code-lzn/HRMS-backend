package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 数据分析 - 核心运营数据汇总
 */
@Data
public class AnalyticsSummaryVO implements Serializable {

    /** 总用户数 */
    private Long totalUsers;
    /** 总用户数环比变化(%) */
    private BigDecimal totalUsersChange;

    /** 活跃用户数 */
    private Long activeUsers;
    /** 活跃用户数环比变化(%) */
    private BigDecimal activeUsersChange;

    /** 总收入 */
    private BigDecimal totalRevenue;
    /** 总收入环比变化(%) */
    private BigDecimal totalRevenueChange;

    /** 平均转化率 */
    private BigDecimal avgConversionRate;
    /** 平均转化率环比变化 */
    private BigDecimal avgConversionRateChange;

    /** 用户留存率 */
    private BigDecimal retentionRate;
    /** 留存率环比变化 */
    private BigDecimal retentionRateChange;

    private static final long serialVersionUID = 1L;
}
