package com.limou.hrms.controller;

import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ResultUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 薪资统计 Controller（AntV 可视化数据源）
 */
@Api(tags = "薪资统计")
@RestController
@RequestMapping("/v1/salary-statistics")
@Slf4j
public class SalaryStatisticsController {

    @ApiOperation("薪资成本月度趋势（近6个月）")
    @GetMapping("/monthly-trend")
    @AuthCheck(mustRole = {UserConstant.HR_ROLE, UserConstant.FINANCE_ROLE})
    public BaseResponse<List<Map<String, Object>>> monthlyTrend() {
        List<Map<String, Object>> data = new ArrayList<>();
        // TODO: 从 salary_batch 表查询近6个月数据，按月份分组汇总
        List<String> months = Arrays.asList("2026-02", "2026-03", "2026-04", "2026-05", "2026-06", "2026-07");
        for (String month : months) {
            Map<String, Object> item = new HashMap<>();
            item.put("month", month);
            item.put("grossPay", BigDecimal.ZERO);
            item.put("netPay", BigDecimal.ZERO);
            item.put("changeRate", "0%");
            data.add(item);
        }
        return ResultUtils.success(data);
    }

    @ApiOperation("部门薪资分布")
    @GetMapping("/department-distribution")
    @AuthCheck(mustRole = {UserConstant.HR_ROLE, UserConstant.FINANCE_ROLE, UserConstant.DEPT_HEAD_ROLE})
    public BaseResponse<List<Map<String, Object>>> departmentDistribution() {
        List<Map<String, Object>> data = new ArrayList<>();
        // TODO: 关联 department 和 salary_detail 统计各部门薪资分布
        return ResultUtils.success(data);
    }

    @ApiOperation("薪资构成占比")
    @GetMapping("/composition")
    @AuthCheck(mustRole = {UserConstant.HR_ROLE, UserConstant.FINANCE_ROLE})
    public BaseResponse<List<Map<String, Object>>> composition() {
        List<Map<String, Object>> data = new ArrayList<>();
        // TODO: 统计当月工资条各项目类型金额占比
        return ResultUtils.success(data);
    }

    @ApiOperation("薪资变动分布")
    @GetMapping("/variation-distribution")
    @AuthCheck(mustRole = {UserConstant.HR_ROLE, UserConstant.FINANCE_ROLE})
    public BaseResponse<List<Map<String, Object>>> variationDistribution() {
        List<Map<String, Object>> data = new ArrayList<>();
        // TODO: 统计员工薪资变动区间分布
        return ResultUtils.success(data);
    }
}
