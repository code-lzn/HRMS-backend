package com.limou.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.limou.hrms.common.BaseResponse;
import com.limou.hrms.common.ResultUtils;
import com.limou.hrms.mapper.*;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工作台 - 数据看板
 */
@RestController
@RequestMapping("/dashboard")
@Slf4j
@RequiredArgsConstructor
public class DashboardController {

    private final UserMapper userMapper;
    private final LoginLogMapper loginLogMapper;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final OperLogMapper operLogMapper;
    private final PageViewLogMapper pageViewLogMapper;

    /**
     * 获取顶部核心指标
     */
    @GetMapping("/metrics")
    public BaseResponse<DashboardMetricsVO> getMetrics() {
        LocalDate today = LocalDate.now();
        Date todayStart = toDate(today);
        Date todayEnd = toDate(today.plusDays(1));
        Date yesterdayStart = toDate(today.minusDays(1));
        Date yesterdayEnd = toDate(today);

        Long totalUsers = userMapper.selectCount(null);

        // 本月新增 vs 上月新增 → 环比增长率
        Date monthStart = toDate(today.with(TemporalAdjusters.firstDayOfMonth()));
        Date nextMonthStart = toDate(today.with(TemporalAdjusters.firstDayOfNextMonth()));
        Date lastMonthStart = toDate(today.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth()));

        Long thisMonthNew = userMapper.selectCount(
                new LambdaQueryWrapper<User>().ge(User::getCreateTime, monthStart).lt(User::getCreateTime, nextMonthStart));
        Long lastMonthNew = userMapper.selectCount(
                new LambdaQueryWrapper<User>().ge(User::getCreateTime, lastMonthStart).lt(User::getCreateTime, monthStart));

        // 今日活跃用户（去重）
        Long activeUsers = distinctUserIdCount(todayStart, todayEnd);

        // 昨日活跃用户
        Long yesterdayActiveUsers = distinctUserIdCount(yesterdayStart, yesterdayEnd);

        // 今日待审批数
        Long pendingApprovals = approvalRecordMapper.selectCount(
                new LambdaQueryWrapper<ApprovalRecord>()
                        .eq(ApprovalRecord::getStatus, "APPROVING"));

        // 昨日新增待审批数
        Long yesterdayPending = approvalRecordMapper.selectCount(
                new LambdaQueryWrapper<ApprovalRecord>()
                        .eq(ApprovalRecord::getStatus, "APPROVING")
                        .ge(ApprovalRecord::getCreateTime, yesterdayStart)
                        .lt(ApprovalRecord::getCreateTime, yesterdayEnd));

        // 今日登录成功率 → 系统健康度
        Long todayLoginTotal = loginLogMapper.selectCount(
                new LambdaQueryWrapper<LoginLog>()
                        .ge(LoginLog::getLoginTime, todayStart)
                        .lt(LoginLog::getLoginTime, todayEnd));
        Long todayLoginSuccess = loginLogMapper.selectCount(
                new LambdaQueryWrapper<LoginLog>()
                        .ge(LoginLog::getLoginTime, todayStart)
                        .lt(LoginLog::getLoginTime, todayEnd)
                        .eq(LoginLog::getIsSuccess, 1));

        Long yesterdayLoginTotal = loginLogMapper.selectCount(
                new LambdaQueryWrapper<LoginLog>()
                        .ge(LoginLog::getLoginTime, yesterdayStart)
                        .lt(LoginLog::getLoginTime, yesterdayEnd));
        Long yesterdayLoginSuccess = loginLogMapper.selectCount(
                new LambdaQueryWrapper<LoginLog>()
                        .ge(LoginLog::getLoginTime, yesterdayStart)
                        .lt(LoginLog::getLoginTime, yesterdayEnd)
                        .eq(LoginLog::getIsSuccess, 1));

        DashboardMetricsVO vo = new DashboardMetricsVO();
        vo.setTotalUsers(totalUsers);
        vo.setTotalUsersGrowth(growthRate(thisMonthNew, lastMonthNew));
        vo.setActiveUsers(activeUsers);
        vo.setActiveUsersGrowth(growthRate(activeUsers, yesterdayActiveUsers));
        vo.setTodayOrders(pendingApprovals);
        vo.setTodayOrdersGrowth(growthRate(pendingApprovals, yesterdayPending));
        vo.setSystemHealth(rate(todayLoginSuccess, todayLoginTotal));
        vo.setSystemHealthChange(change(rate(todayLoginSuccess, todayLoginTotal),
                rate(yesterdayLoginSuccess, yesterdayLoginTotal)));
        return ResultUtils.success(vo);
    }

    /**
     * 获取近7天访问趋势
     */
    @GetMapping("/visit-trend")
    public BaseResponse<List<VisitTrendVO>> getVisitTrend() {
        LocalDate today = LocalDate.now();
        Date sevenDaysAgo = toDate(today.minusDays(6));

        List<PageViewLog> logs = pageViewLogMapper.selectList(
                new LambdaQueryWrapper<PageViewLog>()
                        .ge(PageViewLog::getViewDate, sevenDaysAgo)
                        .orderByAsc(PageViewLog::getViewDate));

        Map<LocalDate, Long> dateCountMap = logs.stream().collect(
                Collectors.toMap(l -> toLocalDate(l.getViewDate()), PageViewLog::getViewCount, (a, b) -> a));

        List<VisitTrendVO> list = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            Long count = dateCountMap.getOrDefault(date, 0L);
            list.add(new VisitTrendVO(dateStr, count));
        }
        return ResultUtils.success(list);
    }

    /**
     * 获取各功能模块使用频率（基于操作日志按模块统计）
     */
    @GetMapping("/module-usage")
    public BaseResponse<List<ModuleUsageVO>> getModuleUsage() {
        List<OperLog> allLogs = operLogMapper.selectList(null);

        Map<String, Long> moduleCount = allLogs.stream()
                .collect(Collectors.groupingBy(OperLog::getModule, Collectors.counting()));

        List<ModuleUsageVO> list = moduleCount.entrySet().stream()
                .map(e -> new ModuleUsageVO(e.getKey(), e.getValue()))
                .sorted((a, b) -> Long.compare(b.getUsageCount(), a.getUsageCount()))
                .collect(Collectors.toList());

        return ResultUtils.success(list);
    }

    /**
     * 获取最近操作动态（最近5条）
     */
    @GetMapping("/recent-logs")
    public BaseResponse<List<RecentLogVO>> getRecentLogs() {
        List<OperLog> logs = operLogMapper.selectList(
                new LambdaQueryWrapper<OperLog>()
                        .orderByDesc(OperLog::getOperateTime)
                        .last("LIMIT 5"));

        List<RecentLogVO> list = logs.stream()
                .map(l -> new RecentLogVO(l.getOperatorName(), l.getAction(),
                        l.getDescription(), l.getOperateTime()))
                .collect(Collectors.toList());

        return ResultUtils.success(list);
    }

    // ========== 工具方法 ==========

    private Long distinctUserIdCount(Date start, Date end) {
        List<LoginLog> logs = loginLogMapper.selectList(
                new LambdaQueryWrapper<LoginLog>()
                        .ge(LoginLog::getLoginTime, start)
                        .lt(LoginLog::getLoginTime, end)
                        .eq(LoginLog::getIsSuccess, 1));
        return logs.stream().map(LoginLog::getUserId).distinct().count();
    }

    private BigDecimal growthRate(Number current, Number previous) {
        double cur = current.doubleValue();
        double prev = previous.doubleValue();
        if (prev == 0) return cur > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        return BigDecimal.valueOf((cur - prev) / prev * 100).setScale(1, RoundingMode.HALF_UP);
    }

    private Integer rate(Long part, Long total) {
        if (total == null || total == 0) return 100;
        return (int) Math.round(part.doubleValue() / total * 100);
    }

    private BigDecimal change(Integer current, Integer previous) {
        if (previous == null || previous == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(current - previous).setScale(1, RoundingMode.HALF_UP);
    }

    private Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
