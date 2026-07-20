package com.limou.hrms.service;

import com.limou.hrms.model.vo.*;

import java.util.List;

/**
 * 薪资统计可视化服务
 */
public interface SalaryStatisticsService {

    /**
     * 近 N 个月薪资成本趋势（折线图）
     * @param months 月数，默认6
     */
    List<SalaryMonthlyTrendVO> getMonthlyTrend(int months);

    /**
     * 指定批次各部门薪资分布（柱状图）
     */
    List<SalaryDeptDistributionVO> getDeptDistribution(Long batchId);

    /**
     * 指定批次薪资构成占比（饼图/环形图）
     */
    List<SalaryCompositionVO> getComposition(Long batchId);

    /**
     * 指定批次社保公积金对比（分组柱状图）
     */
    List<SalarySocialSecurityVO> getSocialSecurityComparison(Long batchId);

    /**
     * 指定批次薪资变动分布（直方图）
     */
    List<SalaryChangeDistributionVO> getChangeDistribution(Long batchId);
}
