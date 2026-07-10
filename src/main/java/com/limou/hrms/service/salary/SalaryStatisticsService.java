package com.limou.hrms.service.salary;

import com.limou.hrms.model.vo.salary.CompositionVO;
import com.limou.hrms.model.vo.salary.CostTrendVO;
import com.limou.hrms.model.vo.salary.DeptDistributionVO;
import com.limou.hrms.model.vo.salary.SalaryTrendVO;
import com.limou.hrms.model.vo.salary.SocialComparisonVO;
import com.limou.hrms.model.vo.salary.VariationDistributionVO;
import java.util.List;

/**
 * 薪资统计服务接口（AntV可视化数据源）
 */
public interface SalaryStatisticsService {

    /**
     * 薪资成本月度趋势（近N个月）
     */
    List<CostTrendVO> getCostTrend(int months);

    /**
     * 部门薪资分布（指定月份）
     */
    List<DeptDistributionVO> getDeptDistribution(String month);

    /**
     * 薪资构成占比（指定月份）
     */
    List<CompositionVO> getComposition(String month);

    /**
     * 社保公积金对比（指定月份）
     */
    List<SocialComparisonVO> getSocialComparison(String month);

    /**
     * 薪资变动分布（指定月份）
     */
    List<VariationDistributionVO> getVariationDistribution(String month);
}
