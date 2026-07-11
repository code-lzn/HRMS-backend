package com.limou.hrms.service.salary.calculator;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 * 薪资计算上下文（汇聚计算所需的所有数据）
 */
@Data
@Builder
public class SalaryCalculationContext {

    /**
     * 员工ID
     */
    private Long employeeId;

    /**
     * 薪资账套ID（用于读取 salary_item 配置）
     */
    private Long accountId;

    /**
     * 基本工资
     */
    private BigDecimal baseSalary;

    /**
     * 津贴基数
     */
    private BigDecimal allowanceBase;

    /**
     * 社保基数
     */
    private BigDecimal socialSecurityBase;

    /**
     * 公积金基数
     */
    private BigDecimal housingFundBase;

    /**
     * 绩效基数
     */
    private BigDecimal performanceBase;

    /**
     * 绩效系数
     */
    private BigDecimal performanceCoefficient;

    /**
     * 是否试用期
     */
    private boolean probation;

    /**
     * 试用期比例
     */
    private BigDecimal probationRatio;

    /**
     * 迟到次数
     */
    private int lateCount;

    /**
     * 请假天数
     */
    private int leaveDays;

    /**
     * 加班小时数（平日）
     */
    private int overtimeHours;

    /**
     * 计算月份 (1-12)
     */
    private int month;

    /**
     * 上月实发薪资（用于异常检测）
     */
    private BigDecimal lastMonthNetPay;

    // ==================== 从 salary_item 加载的配置 ====================

    /**
     * 社保总费率（所有 SOCIAL_SECURITY 类型 item 的 formula 之和）
     */
    private BigDecimal socialSecurityRate;

    /**
     * 公积金总费率（所有 HOUSING_FUND 类型 item 的 formula 之和）
     */
    private BigDecimal housingFundRate;

    /**
     * 迟到每次扣款金额（元）
     */
    private BigDecimal lateFinePerTime;

    /**
     * 加班费倍数
     */
    private BigDecimal overtimeMultiplier;

    /**
     * 日工资（baseSalary / 21.75）
     */
    public BigDecimal getDailySalary() {
        if (baseSalary == null) {
            return BigDecimal.ZERO;
        }
        return baseSalary.divide(new BigDecimal("21.75"), 2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 小时工资（日工资 / 8）
     */
    public BigDecimal getHourlySalary() {
        return getDailySalary().divide(new BigDecimal("8"), 2, BigDecimal.ROUND_HALF_UP);
    }
}
