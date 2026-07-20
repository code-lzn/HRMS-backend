package com.limou.hrms.controller;

import com.limou.hrms.annotation.AuthCheck;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.constant.UserConstant;
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
     * <p>
     * 权限：HR/管理员/部门主管。部门主管仅看管辖部门及子部门。
     */
    @GetMapping("/charts/attendance-rate")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE, UserConstant.DEPT_HEAD_ROLE})
    public BaseResponse<AttendanceRateChartVO> getAttendanceRate(
            @RequestParam(defaultValue = "6") int months,
            @RequestParam(required = false) List<Long> departmentIds) {
        log.info("{} 查询出勤率趋势, months={}, departmentIds={}", UserContext.getCurrentUser(), months, departmentIds);
        AttendanceRateChartVO vo = statisticsService.getAttendanceRate(months, departmentIds);
        return ResultUtils.success(vo);
    }

    /**
     * GET /api/attendance/statistics/charts/leave-distribution — 请假类型分布（饼图/环形图）
     * <p>
     * 权限：HR/管理员。统计当月已通过请假申请的各类型占比。
     */
    @GetMapping("/charts/leave-distribution")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE})
    public BaseResponse<List<LeaveDistributionVO>> getLeaveDistribution(
            @RequestParam int year,
            @RequestParam int month) {
        log.info("{} 查询请假类型分布, year={}, month={}", UserContext.getCurrentUser(), year, month);
        List<LeaveDistributionVO> list = statisticsService.getLeaveDistribution(year, month);
        return ResultUtils.success(list);
    }

    /**
     * GET /api/attendance/statistics/charts/late-early-ranking — 迟到早退排行榜（柱状图）
     * <p>
     * 权限：HR/管理员/部门主管。部门维度迟到早退人次对比。部门主管仅看管辖部门。
     */
    @GetMapping("/charts/late-early-ranking")
    @AuthCheck(mustRole = {UserConstant.ADMIN_ROLE, UserConstant.HR_ROLE, UserConstant.DEPT_HEAD_ROLE})
    public BaseResponse<List<LeaveEarlyRankingVO>> getLateEarlyRanking(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(defaultValue = "10") int topN) {
        log.info("{} 查询迟到早退排行, year={}, month={}, topN={}", UserContext.getCurrentUser(), year, month, topN);
        List<LeaveEarlyRankingVO> list = statisticsService.getLateEarlyRanking(year, month, topN);
        return ResultUtils.success(list);
    }
}
