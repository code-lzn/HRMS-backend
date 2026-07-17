package com.limou.hrms.controller;

import com.limou.hrms.annotation.AuthCheck;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.context.UserContext;
import com.limou.hrms.model.vo.AttendanceRateChartVO;
import com.limou.hrms.model.vo.LeaveDistributionVO;
import com.limou.hrms.model.vo.LeaveEarlyRankingVO;
import com.limou.hrms.service.AttendanceStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 考勤统计控制器 — 图表可视化数据
 */
@RestController
@RequestMapping("/attendance/statistics")
@Slf4j
@RequiredArgsConstructor
public class AttendanceStatisticsController {

    private final AttendanceStatisticsService statisticsService;

    /**
     * GET /api/attendance/statistics/charts/attendance-rate — 部门出勤率趋势（折线图）
     */
    @GetMapping("/charts/attendance-rate")
    @AuthCheck
    public BaseResponse<AttendanceRateChartVO> getAttendanceRate(
            @RequestParam(defaultValue = "6") int months,
            @RequestParam(required = false) List<Long> departmentIds) {
        log.info("{} 查询出勤率趋势, months={}, departmentIds={}", UserContext.getCurrentUser(), months, departmentIds);
        AttendanceRateChartVO vo = statisticsService.getAttendanceRate(months, departmentIds);
        return ResultUtils.success(vo);
    }

    /**
     * GET /api/attendance/statistics/charts/leave-distribution — 请假类型分布（饼图/环形图）
     */
    @GetMapping("/charts/leave-distribution")
    @AuthCheck
    public BaseResponse<List<LeaveDistributionVO>> getLeaveDistribution(
            @RequestParam int year,
            @RequestParam int month) {
        log.info("{} 查询请假类型分布, year={}, month={}", UserContext.getCurrentUser(), year, month);
        List<LeaveDistributionVO> list = statisticsService.getLeaveDistribution(year, month);
        return ResultUtils.success(list);
    }

    /**
     * GET /api/attendance/statistics/charts/late-early-ranking — 迟到早退排行榜（柱状图）
     */
    @GetMapping("/charts/late-early-ranking")
    @AuthCheck
    public BaseResponse<List<LeaveEarlyRankingVO>> getLateEarlyRanking(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(defaultValue = "10") int topN) {
        log.info("{} 查询迟到早退排行, year={}, month={}, topN={}", UserContext.getCurrentUser(), year, month, topN);
        List<LeaveEarlyRankingVO> list = statisticsService.getLateEarlyRanking(year, month, topN);
        return ResultUtils.success(list);
    }
}