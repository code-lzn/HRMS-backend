package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.limou.hrms.constant.DataScopeContext;
import com.limou.hrms.constant.DataScopeEnum;
import com.limou.hrms.mapper.*;
import com.limou.hrms.model.entity.*;
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

    private final AttendanceRecordMapper attendanceRecordMapper;
    private final LeaveRequestMapper leaveRequestMapper;
    private final EmployeeWorkInfoMapper employeeWorkInfoMapper;
    private final DepartmentService departmentService;
    private final DataScopeContext dataScopeContext;

    @Override
    public AttendanceRateChartVO getAttendanceRate(int months, List<Long> departmentIds) {
        List<String> monthLabels = new ArrayList<>();
        List<Integer> years = new ArrayList<>();
        List<Integer> monthVals = new ArrayList<>();
        LocalDate now = LocalDate.now();

        for (int i = months - 1; i >= 0; i--) {
            LocalDate d = now.minusMonths(i);
            monthLabels.add(String.format("%d-%02d", d.getYear(), d.getMonthValue()));
            years.add(d.getYear());
            monthVals.add(d.getMonthValue());
        }

        // 部门主管数据范围
        Set<Long> allowedDeptIds = resolveManagedDeptIdsIfNeeded();
        if (departmentIds != null && !departmentIds.isEmpty()) {
            if (allowedDeptIds != null) {
                departmentIds.retainAll(allowedDeptIds);
            }
        } else {
            departmentIds = new ArrayList<>(allowedDeptIds != null
                    ? allowedDeptIds : loadAllDeptIds());
        }
        if (departmentIds.isEmpty()) {
            return emptyRateVO(monthLabels);
        }

        LocalDate startDate = LocalDate.of(years.get(0), monthVals.get(0), 1);
        LocalDate endDate = LocalDate.of(years.get(years.size() - 1), monthVals.get(monthVals.size() - 1), 1)
                .plusMonths(1).minusDays(1);

        // 批量查所有记录 + 员工→部门映射
        List<AttendanceRecord> records = attendanceRecordMapper.selectList(
                Wrappers.<AttendanceRecord>lambdaQuery()
                        .between(AttendanceRecord::getAttendanceDate, startDate, endDate));
        Set<Long> empIds = records.stream().map(AttendanceRecord::getEmployeeId).collect(Collectors.toSet());
        Map<Long, Long> empDeptMap = loadEmpDeptMap(empIds);

        List<SeriesItem> series = new ArrayList<>();
        for (Long deptId : departmentIds) {
            Department dept = departmentService.getById(deptId);
            String deptName = dept != null ? dept.getName() : "未知部门";

            Set<Long> deptEmpIds = empDeptMap.entrySet().stream()
                    .filter(e -> e.getValue().equals(deptId))
                    .map(Map.Entry::getKey).collect(Collectors.toSet());

            List<BigDecimal> rates = new ArrayList<>();
            for (int i = 0; i < months; i++) {
                int y = years.get(i), m = monthVals.get(i);
                long totalRecords = records.stream()
                        .filter(r -> deptEmpIds.contains(r.getEmployeeId())
                                && r.getAttendanceDate().getYear() == y
                                && r.getAttendanceDate().getMonthValue() == m)
                        .count();
                long presentRecords = records.stream()
                        .filter(r -> deptEmpIds.contains(r.getEmployeeId())
                                && r.getAttendanceDate().getYear() == y
                                && r.getAttendanceDate().getMonthValue() == m
                                && isPresent(r))
                        .count();
                BigDecimal rate = totalRecords > 0
                        ? BigDecimal.valueOf(presentRecords).divide(BigDecimal.valueOf(totalRecords), 4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                rates.add(rate);
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

    @Override
    public List<LeaveDistributionVO> getLeaveDistribution(int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        // 已通过的请假记录
        List<LeaveRequest> leaves = leaveRequestMapper.selectList(
                Wrappers.<LeaveRequest>lambdaQuery()
                        .eq(LeaveRequest::getStatus, 3)
                        .ge(LeaveRequest::getStartTime, startDate.atStartOfDay())
                        .le(LeaveRequest::getStartTime, endDate.atTime(23, 59, 59)));

        BigDecimal annual = BigDecimal.ZERO, sick = BigDecimal.ZERO, personal = BigDecimal.ZERO,
                marriage = BigDecimal.ZERO, maternity = BigDecimal.ZERO, funeral = BigDecimal.ZERO,
                comp = BigDecimal.ZERO;

        for (LeaveRequest lv : leaves) {
            BigDecimal days = lv.getLeaveDays() != null ? lv.getLeaveDays() : BigDecimal.ZERO;
            switch (lv.getLeaveType()) {
                case 1: annual = annual.add(days); break;
                case 2: sick = sick.add(days); break;
                case 3: personal = personal.add(days); break;
                case 4: marriage = marriage.add(days); break;
                case 5: maternity = maternity.add(days); break;
                case 6: funeral = funeral.add(days); break;
                case 7: comp = comp.add(days); break;
            }
        }

        BigDecimal total = annual.add(sick).add(personal).add(marriage).add(maternity).add(funeral).add(comp);
        BigDecimal zero = BigDecimal.ZERO;
        if (total.compareTo(zero) == 0) total = BigDecimal.ONE; // 避免除以0

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

    @Override
    public List<LeaveEarlyRankingVO> getLateEarlyRanking(int year, int month, int topN) {
        Set<Long> allowedDeptIds = resolveManagedDeptIdsIfNeeded();

        // 当月所有打卡记录
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        List<AttendanceRecord> records = attendanceRecordMapper.selectList(
                Wrappers.<AttendanceRecord>lambdaQuery()
                        .between(AttendanceRecord::getAttendanceDate, startDate, endDate));
        Set<Long> empIds = records.stream().map(AttendanceRecord::getEmployeeId).collect(Collectors.toSet());
        Map<Long, Long> empDeptMap = loadEmpDeptMap(empIds);

        Map<Long, Integer> lateMap = new HashMap<>();
        Map<Long, Integer> earlyMap = new HashMap<>();

        for (AttendanceRecord r : records) {
            Long deptId = empDeptMap.get(r.getEmployeeId());
            if (deptId == null) continue;
            if (allowedDeptIds != null && !allowedDeptIds.contains(deptId)) continue;

            if (r.getStartStatus() != null && r.getStartStatus() == 2) {
                lateMap.merge(deptId, 1, Integer::sum);
            }
            if (r.getEndStatus() != null && r.getEndStatus() == 2) {
                earlyMap.merge(deptId, 1, Integer::sum);
            }
        }

        Set<Long> allDeptIds = new HashSet<>();
        allDeptIds.addAll(lateMap.keySet());
        allDeptIds.addAll(earlyMap.keySet());

        return allDeptIds.stream().map(deptId -> {
            LeaveEarlyRankingVO vo = new LeaveEarlyRankingVO();
            Department dept = departmentService.getById(deptId);
            vo.setDepartmentName(dept != null ? dept.getName() : "未知部门");
            vo.setLateCount(lateMap.getOrDefault(deptId, 0));
            vo.setEarlyLeaveCount(earlyMap.getOrDefault(deptId, 0));
            return vo;
        }).sorted((a, b) -> Integer.compare(
                b.getLateCount() + b.getEarlyLeaveCount(),
                a.getLateCount() + a.getEarlyLeaveCount()))
                .limit(topN)
                .collect(Collectors.toList());
    }

    // ==================== 工具 ====================

    /** 部门主管返回管辖部门集合，HR/管理员返回 null（全量） */
    private Set<Long> resolveManagedDeptIdsIfNeeded() {
        DataScopeEnum scope = dataScopeContext.getAttendanceScope();
        if (scope == DataScopeEnum.DEPT) {
            return dataScopeContext.getManagedDepartmentIds();
        }
        return null;
    }

    private Set<Long> loadAllDeptIds() {
        return departmentService.list().stream()
                .map(Department::getId).collect(Collectors.toSet());
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

    private boolean isPresent(AttendanceRecord r) {
        return (r.getStartStatus() != null && r.getStartStatus() <= 2)
                || (r.getEndStatus() != null && r.getEndStatus() <= 2);
    }

    private AttendanceRateChartVO emptyRateVO(List<String> monthLabels) {
        AttendanceRateChartVO vo = new AttendanceRateChartVO();
        vo.setMonths(monthLabels);
        vo.setSeries(Collections.emptyList());
        return vo;
    }

    private void addItem(List<LeaveDistributionVO> list, String desc, BigDecimal days, BigDecimal total) {
        LeaveDistributionVO vo = new LeaveDistributionVO();
        vo.setLeaveTypeDesc(desc);
        vo.setDays(days);
        vo.setPercentage(days.divide(total, 4, RoundingMode.HALF_UP));
        list.add(vo);
    }
}
