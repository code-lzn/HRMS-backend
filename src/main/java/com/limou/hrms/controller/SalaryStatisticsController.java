package com.limou.hrms.controller;

import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.model.vo.*;
import com.limou.hrms.service.SalaryStatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 薪资统计可视化接口（管理端 AntV 图表数据源）
 */
@RestController
@RequestMapping("/salary-statistics")
@Slf4j
public class SalaryStatisticsController {

    @Resource
    private SalaryStatisticsService salaryStatisticsService;

    /**
     * 近 N 个月薪资成本月度趋势（折线图）
     */
    @GetMapping("/monthly-trend")
    public BaseResponse<List<SalaryMonthlyTrendVO>> getMonthlyTrend(
            @RequestParam(defaultValue = "6") int months) {
        return ResultUtils.success(salaryStatisticsService.getMonthlyTrend(months));
    }

    /**
     * 指定批次各部门薪资分布（柱状图）
     */
    @GetMapping("/dept-distribution")
    public BaseResponse<List<SalaryDeptDistributionVO>> getDeptDistribution(
            @RequestParam Long batchId) {
        return ResultUtils.success(salaryStatisticsService.getDeptDistribution(batchId));
    }

    /**
     * 指定批次薪资构成占比（饼图/环形图）
     */
    @GetMapping("/composition")
    public BaseResponse<List<SalaryCompositionVO>> getComposition(
            @RequestParam Long batchId) {
        return ResultUtils.success(salaryStatisticsService.getComposition(batchId));
    }

    /**
     * 指定批次社保公积金对比（分组柱状图）
     */
    @GetMapping("/social-security")
    public BaseResponse<List<SalarySocialSecurityVO>> getSocialSecurityComparison(
            @RequestParam Long batchId) {
        return ResultUtils.success(salaryStatisticsService.getSocialSecurityComparison(batchId));
    }

    /**
     * 指定批次薪资变动分布（直方图）
     */
    @GetMapping("/change-distribution")
    public BaseResponse<List<SalaryChangeDistributionVO>> getChangeDistribution(
            @RequestParam Long batchId) {
        return ResultUtils.success(salaryStatisticsService.getChangeDistribution(batchId));
    }
}
