package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 工作台顶部核心指标
 */
@Data
public class DashboardMetricsVO implements Serializable {

    /** 总用户数 */
    private Long totalUsers;
    /** 总用户数环比增长率 */
    private BigDecimal totalUsersGrowth;

    /** 活跃用户数 */
    private Long activeUsers;
    /** 活跃用户环比增长率 */
    private BigDecimal activeUsersGrowth;

    /** 今日订单数 */
    private Long todayOrders;
    /** 今日订单环比增长率 */
    private BigDecimal todayOrdersGrowth;

    /** 系统健康度 (0-100) */
    private Integer systemHealth;
    /** 系统健康度变化 */
    private BigDecimal systemHealthChange;

    private static final long serialVersionUID = 1L;
}
