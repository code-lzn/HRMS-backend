package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.limou.hrms.mapper.AttendanceStatisticsMapper;
import com.limou.hrms.mapper.EmployeeWorkInfoMapper;
import com.limou.hrms.model.entity.AttendanceStatistics;
import com.limou.hrms.model.entity.EmployeeWorkInfo;
import com.limou.hrms.model.vo.AttendanceRateChartVO;
import com.limou.hrms.model.vo.AttendanceRateChartVO.SeriesItem;
import com.limou.hrms.model.vo.LeaveDistributionVO;
import com.limou.hrms.model.vo.LeaveEarlyRankingVO;
import com.limou.hrms.service.AttendanceStatisticsService;
import com.limou.hrms.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AttendanceStatisticsServiceImpl implements AttendanceStatisticsService {

    private final AttendanceStatisticsMapper statisticsMapper;
    private final EmployeeWorkInfoMapper employeeWorkInfoMapper;
    private final DepartmentService departmentService;

    @Override
    public AttendanceRateChartVO getAttendanceRate(int months, List<Long> departmentIds) {
        LocalDate now = LocalDate.now();
        List<String> monthLabels = new ArrayList<>();
        List<Integer> years = new ArrayList<>();
        List<Integer> monthVals = new ArrayList<>();

        for (int i = months - 1; i >= 0; i--) {
            LocalDate d = now.minusMonths(i);
            monthLabels.add(String.format("%d-%02d", d.getYear(), d.getMonthValue()));
            years.add(d.getYear());
            monthVals.add(d.getMonthValue());
        }

        // 查所有统计记录
        List<AttendanceStatistics> allStats = statisticsMapper.selectList(
                Wrappers.<AttendanceStatistics>lambdaQuery()
                        .ge(AttendanceStatistics::getStatYear, years.get(0))
                        .le(AttendanceStatistics::getStatYear, years.get(years.size() - 1)));

        // 员工 → 部门映射
        Set<Long> empIds = allStats.stream().map(AttendanceStatistics::getEmployeeId).collect(Collectors.toSet());
        Map<Long, Long> empDeptMap = loadEmpDeptMap(empIds);

        Set<Long> targetDeptIds = departmentIds != null && !departmentIds.isEmpty()
                ? new HashSet<>(departmentIds) : new HashSet<>(empDeptMap.values());

        List<SeriesItem> series = new ArrayList<>();
        for (Long deptId : targetDeptIds) {
            String deptName = departmentService.getById(deptId) != null
                    ? departmentService.getById(deptId).getName() : "未知部门";

            Set<Long> deptEmpIds = empDeptMap.entrySet().stream()
                    .filter(e -> e.getValue().equals(deptId))
                    .map(Map.Entry::getKey).collect(Collectors.toSet());

            List<BigDecimal> rates = new ArrayList<>();
            for (int i = 0; i < months; i++) {
                int y = years.get(i);
                int m = monthVals.get(i);
                double avg = allStats.stream()
                        .filter(s -> s.getStatYear() == y && s.getStatMonth() == m
                                && deptEmpIds.contains(s.getEmployeeId())
                                && s.getScheduledDays() != null
                                && s.getScheduledDays().compareTo(BigDecimal.ZERO) > 0)
                        .mapToDouble(s -> s.getActualDays() != null
                                ? s.getActualDays().divide(s.getScheduledDays(), 4, RoundingMode.HALF_UP).doubleValue()
                                : 0)
                        .average().orElse(0);
                rates.add(BigDecimal.valueOf(avg).setScale(4, RoundingMode.HALF_UP));
            }

            SeriesItem item = new SeriesItem();
            item.setDepartmentName(deptName);
            item.setRates(rates);
            series.add(item);
        }

        AttendanceRateChartVO vo = new AttendanceRateChartVO();
        vo.setMonths(monthLabels);
        vo.setSeries(series);
        return vo;
    }

    private Map<Long, Long> loadEmpDeptMap(Set<Long> empIds) {
        if (empIds.isEmpty()) return Collections.emptyMap();
        return employeeWorkInfoMapper.selectList(
                        Wrappers.<EmployeeWorkInfo>lambdaQuery()
                                .in(EmployeeWorkInfo::getEmployeeId, empIds))
                .stream()
                .collect(Collectors.toMap(
                        EmployeeWorkInfo::getEmployeeId, EmployeeWorkInfo::getDepartmentId, (a, b) -> a));
    }

    @Override
    public List<LeaveDistributionVO> getLeaveDistribution(int year, int month) {
        List<AttendanceStatistics> stats = statisticsMapper.selectList(
                Wrappers.<AttendanceStatistics>lambdaQuery()
                        .eq(AttendanceStatistics::getStatYear, year)
                        .eq(AttendanceStatistics::getStatMonth, month));

        BigDecimal annual = sum(stats, AttendanceStatistics::getAnnualLeaveDays);
        BigDecimal sick = sum(stats, AttendanceStatistics::getSickLeaveDays);
        BigDecimal personal = sum(stats, AttendanceStatistics::getPersonalLeaveDays);
        BigDecimal marriage = sum(stats, AttendanceStatistics::getMarriageLeaveDays);
        BigDecimal maternity = sum(stats, AttendanceStatistics::getMaternityLeaveDays);
        BigDecimal funeral = sum(stats, AttendanceStatistics::getFuneralLeaveDays);
        BigDecimal comp = sum(stats, AttendanceStatistics::getCompTimeLeaveDays);
        BigDecimal total = annual.add(sick).add(personal).add(marriage).add(maternity).add(funeral).add(comp);

        List<LeaveDistributionVO> result = new ArrayList<>();
        addItem(result, "年假", annual, total);
        addItem(result, "病假", sick, total);
        addItem(result, "事假", personal, total);
        addItem(result, "婚假", marriage, total);
        addItem(result, "产假", maternity, total);
        addItem(result, "丧假", funeral, total);
        addItem(result, "调休", comp, total);
        return result;
    }

    private BigDecimal sum(List<AttendanceStatistics> stats, java.util.function.Function<AttendanceStatistics, BigDecimal> getter) {
        return stats.stream().map(getter).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void addItem(List<LeaveDistributionVO> list, String desc, BigDecimal days, BigDecimal total) {
        LeaveDistributionVO vo = new LeaveDistributionVO();
        vo.setLeaveTypeDesc(desc);
        vo.setDays(days);
        vo.setPercentage(total.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : days.divide(total, 4, RoundingMode.HALF_UP));
        list.add(vo);
    }

    @Override
    public List<LeaveEarlyRankingVO> getLateEarlyRanking(int year, int month, int topN) {
        List<AttendanceStatistics> stats = statisticsMapper.selectList(
                Wrappers.<AttendanceStatistics>lambdaQuery()
                        .eq(AttendanceStatistics::getStatYear, year)
                        .eq(AttendanceStatistics::getStatMonth, month));

        Set<Long> empIds = stats.stream().map(AttendanceStatistics::getEmployeeId).collect(Collectors.toSet());
        Map<Long, Long> empDeptMap = loadEmpDeptMap(empIds);

        Map<Long, Integer> lateMap = new HashMap<>();
        Map<Long, Integer> earlyMap = new HashMap<>();

        for (AttendanceStatistics s : stats) {
            Long deptId = empDeptMap.get(s.getEmployeeId());
            if (deptId == null) continue;
            lateMap.merge(deptId, s.getLateCount() != null ? s.getLateCount() : 0, Integer::sum);
            earlyMap.merge(deptId, s.getEarlyLeaveCount() != null ? s.getEarlyLeaveCount() : 0, Integer::sum);
        }

        Set<Long> allDeptIds = new HashSet<>();
        allDeptIds.addAll(lateMap.keySet());
        allDeptIds.addAll(earlyMap.keySet());

        return allDeptIds.stream().map(deptId -> {
            LeaveEarlyRankingVO vo = new LeaveEarlyRankingVO();
            vo.setDepartmentName(departmentService.getById(deptId) != null
                    ? departmentService.getById(deptId).getName() : "未知部门");
            vo.setLateCount(lateMap.getOrDefault(deptId, 0));
            vo.setEarlyLeaveCount(earlyMap.getOrDefault(deptId, 0));
            return vo;
        }).sorted((a, b) -> Integer.compare(
                b.getLateCount() + b.getEarlyLeaveCount(),
                a.getLateCount() + a.getEarlyLeaveCount()))
                .limit(topN)
                .collect(Collectors.toList());
    }
}