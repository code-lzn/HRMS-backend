package com.limou.hrms.controller;

import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.model.vo.salary.CompositionVO;
import com.limou.hrms.model.vo.salary.CostTrendVO;
import com.limou.hrms.model.vo.salary.DeptDistributionVO;
import com.limou.hrms.model.vo.salary.SocialComparisonVO;
import com.limou.hrms.model.vo.salary.VariationDistributionVO;
import com.limou.hrms.service.salary.SalaryStatisticsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 薪资统计 Controller（AntV 可视化数据源）
 */
@Api(tags = "薪资统计（AntV可视化）")
@RestController
@RequestMapping("/v1/salary-statistics")
@Slf4j
public class SalaryStatisticsController {

    @Resource
    private SalaryStatisticsService salaryStatisticsService;

    @ApiOperation("薪资成本月度趋势（折线图）")
    @GetMapping("/cost-trend")
    public BaseResponse<List<CostTrendVO>> getCostTrend(
            @ApiParam("月数，默认6") @RequestParam(defaultValue = "6") int months) {
        List<CostTrendVO> trend = salaryStatisticsService.getCostTrend(months);
        return ResultUtils.success(trend);
    }

    @ApiOperation("部门薪资分布（分组柱状图）")
    @GetMapping("/dept-distribution")
    public BaseResponse<List<DeptDistributionVO>> getDeptDistribution(
            @ApiParam("月份，格式yyyy-MM，不传则取最新") @RequestParam(required = false) String month) {
        List<DeptDistributionVO> distribution = salaryStatisticsService.getDeptDistribution(month);
        return ResultUtils.success(distribution);
    }

    @ApiOperation("薪资构成占比（饼图）")
    @GetMapping("/composition")
    public BaseResponse<List<CompositionVO>> getComposition(
            @ApiParam("月份，格式yyyy-MM，不传则取最新") @RequestParam(required = false) String month) {
        List<CompositionVO> composition = salaryStatisticsService.getComposition(month);
        return ResultUtils.success(composition);
    }

    @ApiOperation("社保公积金对比（分组柱状图）")
    @GetMapping("/social-comparison")
    public BaseResponse<List<SocialComparisonVO>> getSocialComparison(
            @ApiParam("月份，格式yyyy-MM，不传则取最新") @RequestParam(required = false) String month) {
        List<SocialComparisonVO> comparison = salaryStatisticsService.getSocialComparison(month);
        return ResultUtils.success(comparison);
    }

    @ApiOperation("薪资变动分布（直方图）")
    @GetMapping("/variation-distribution")
    public BaseResponse<List<VariationDistributionVO>> getVariationDistribution(
            @ApiParam("月份，格式yyyy-MM，不传则取最新") @RequestParam(required = false) String month) {
        List<VariationDistributionVO> distribution = salaryStatisticsService.getVariationDistribution(month);
        return ResultUtils.success(distribution);
    }
}
