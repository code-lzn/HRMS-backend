package com.limou.hrms.service.impl;

import cn.hutool.core.date.DateUtil;
import com.limou.hrms.mapper.AttendanceMapper;
import com.limou.hrms.mapper.EmployeeMapper;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.AttendanceStatusEnum;
import com.limou.hrms.model.vo.*;
import com.limou.hrms.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AttendanceStatsServiceImpl implements AttendanceStatsService {

    @Resource
    private AttendanceService attendanceService;

    @Resource
    private EmployeeService employeeService;

    @Resource
    private DepartmentService departmentService;

    @Resource
    private LeaveService leaveService;

    @Resource
    private EmployeeLeaveBalanceService employeeLeaveBalanceService;

    @Resource
    private EmployeeMapper employeeMapper;

    @Resource
    private HolidayConfigService holidayConfigService;

    @Resource
    private AttendanceMapper attendanceMapper;

    @Override
    public AttendanceStatsVO getPersonalStats(Long userId, String month) {
        Employee emp = employeeService.getByUserId(userId);
        if (emp == null) return null;

        Date monthStart = DateUtil.parseDate(month + "-01");
        Date monthEnd = DateUtil.endOfMonth(monthStart);

        List<Attendance> records = attendanceService.lambdaQuery()
                .eq(Attendance::getEmployeeId, emp.getId())
                .ge(Attendance::getAttendanceDate, DateUtil.formatDate(monthStart))
                .le(Attendance::getAttendanceDate, DateUtil.formatDate(monthEnd))
                .list();

        AttendanceStatsVO vo = new AttendanceStatsVO();
        vo.setEmployeeId(emp.getId());
        vo.setEmployeeName(emp.getEmployeeName());

        if (emp.getDepartmentId() != null) {
            Department dept = departmentService.getById(emp.getDepartmentId());
            vo.setDepartmentName(dept != null ? dept.getDeptName() : null);
        }

        int totalDays = countWorkDays(monthStart, monthEnd);
        vo.setTotalDays(totalDays);

        int normalDays = 0, lateDays = 0, earlyDays = 0, missingDays = 0, leaveDays = 0, absentDays = 0;

        for (Attendance r : records) {
            Integer st = r.getStatus();
            if (st == null) continue;
            switch (st) {
                case 0: normalDays++; break;
                case 1: lateDays++; break;
                case 2: earlyDays++; break;
                case 3: missingDays++; break;
                case 4: leaveDays++; break;
                case 5: absentDays++; break;
                case 6: case 7: missingDays++; break;   // 上班缺卡/下班缺卡 → 缺卡
                case 9: lateDays++; earlyDays++; break; // 迟到&早退 → 各算一次
                default: break;
            }
        }

        vo.setNormalDays(normalDays);
        vo.setLateDays(lateDays);
        vo.setEarlyDays(earlyDays);
        vo.setMissingDays(missingDays);
        vo.setLeaveDays(leaveDays);
        vo.setAbsentDays(absentDays);

        int actualDays = (int) records.stream()
                .filter(r -> r.getStatus() != null && r.getStatus() != 8
                        && r.getStatus() != 3 && r.getStatus() != 6 && r.getStatus() != 7
                        && r.getStatus() != 4 && r.getStatus() != 5)
                .count();
        double rate = totalDays > 0 ? (actualDays * 100.0 / totalDays) : 0;
        vo.setAttendanceRate(BigDecimal.valueOf(rate).setScale(2, RoundingMode.HALF_UP).doubleValue());

        // 加班时长：当月已审批的加班时长汇总
        double overtimeTotal = records.stream()
                .filter(r -> r.getOvertimeHours() != null)
                .mapToDouble(Attendance::getOvertimeHours)
                .sum();
        vo.setOvertimeHours(BigDecimal.valueOf(overtimeTotal).setScale(1, RoundingMode.HALF_UP).doubleValue());

        // 年假余额
        LeaveBalanceVO balance = employeeLeaveBalanceService.getCurrentYearBalance(emp.getId());
        vo.setAnnualLeaveBalance(balance != null && balance.getAnnualRemaining() != null
                ? balance.getAnnualRemaining().doubleValue() : 0.0);

        // 请假天数汇总（从leave_record表统计已批准的请假天数）
        List<Leave> monthLeaves = leaveService.lambdaQuery()
                .eq(Leave::getEmployeeId, emp.getId())
                .eq(Leave::getStatus, 1) // 已批准
                .ge(Leave::getStartDate, DateUtil.formatDate(monthStart))
                .le(Leave::getEndDate, DateUtil.formatDate(monthEnd))
                .list();
        double leaveDaysTotal = monthLeaves.stream()
                .filter(l -> l.getTotalDays() != null)
                .mapToDouble(l -> l.getTotalDays().doubleValue())
                .sum();
        vo.setTotalLeaveDays((int) Math.ceil(leaveDaysTotal));

        return vo;
    }

    @Override
    public List<DepartmentAttendanceStatsVO> getDepartmentStats(String month) {
        Date monthStart = DateUtil.parseDate(month + "-01");
        Date monthEnd = DateUtil.endOfMonth(monthStart);
        int totalWorkDays = countWorkDays(monthStart, monthEnd);

        List<Department> departments = departmentService.lambdaQuery()
                .eq(Department::getIsDeleted, 0)
                .list();

        Map<Long, String> deptNameMap = departments.stream()
                .collect(Collectors.toMap(Department::getId, Department::getDeptName));

        Map<Long, Integer> deptEmployeeCountMap = employeeService.lambdaQuery()
                .eq(Employee::getIsDeleted, 0)
                .list().stream()
                .filter(e -> e.getDepartmentId() != null)
                .collect(Collectors.groupingBy(Employee::getDepartmentId, Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        List<Map<String, Object>> statsList = attendanceMapper.getDepartmentAttendanceStats(
                DateUtil.formatDate(monthStart), DateUtil.formatDate(monthEnd));

        Map<Long, Map<String, Object>> deptStatsMap = statsList.stream()
                .collect(Collectors.toMap(m -> ((Number) m.get("departmentId")).longValue(), m -> m));

        List<DepartmentAttendanceStatsVO> result = new ArrayList<>();
        for (Department dept : departments) {
            DepartmentAttendanceStatsVO vo = new DepartmentAttendanceStatsVO();
            vo.setDepartmentId(dept.getId());
            vo.setDepartmentName(dept.getDeptName());

            int empCount = deptEmployeeCountMap.getOrDefault(dept.getId(), 0);
            vo.setEmployeeCount(empCount);
            vo.setTotalWorkDays(totalWorkDays);

            Map<String, Object> stats = deptStatsMap.get(dept.getId());
            if (stats != null) {
                vo.setActualAttendanceDays(((Number) stats.getOrDefault("attendanceDays", 0)).intValue());
                vo.setLateCount(((Number) stats.getOrDefault("lateCount", 0)).intValue());
                vo.setEarlyCount(((Number) stats.getOrDefault("earlyCount", 0)).intValue());
                vo.setLeaveDays(((Number) stats.getOrDefault("leaveDays", 0)).intValue());
                vo.setAbsentDays(((Number) stats.getOrDefault("absentDays", 0)).intValue());

                double totalShouldDays = totalWorkDays * empCount;
                double attendanceRate = totalShouldDays > 0
                        ? (((Number) stats.getOrDefault("attendanceDays", 0)).intValue() * 100.0 / totalShouldDays) : 0;
                vo.setAttendanceRate(BigDecimal.valueOf(attendanceRate).setScale(2, RoundingMode.HALF_UP).doubleValue());

                double lateRate = empCount > 0
                        ? (((Number) stats.getOrDefault("lateEmployeeCount", 0)).intValue() * 100.0 / empCount) : 0;
                vo.setLateRate(BigDecimal.valueOf(lateRate).setScale(2, RoundingMode.HALF_UP).doubleValue());

                double leaveRate = empCount > 0
                        ? (((Number) stats.getOrDefault("leaveEmployeeCount", 0)).intValue() * 100.0 / empCount) : 0;
                vo.setLeaveRate(BigDecimal.valueOf(leaveRate).setScale(2, RoundingMode.HALF_UP).doubleValue());
            } else {
                vo.setActualAttendanceDays(0);
                vo.setLateCount(0);
                vo.setEarlyCount(0);
                vo.setLeaveDays(0);
                vo.setAbsentDays(0);
                vo.setAttendanceRate(0.0);
                vo.setLateRate(0.0);
                vo.setLeaveRate(0.0);
            }

            result.add(vo);
        }

        return result;
    }

    @Override
    public AttendanceTrendVO getAttendanceTrend(Long departmentId, Integer months, String endMonth) {
        AttendanceTrendVO vo = new AttendanceTrendVO();
        List<String> monthList = new ArrayList<>();
        List<Double> rateList = new ArrayList<>();

        boolean allDepts = (departmentId == null || departmentId <= 0);

        Calendar cal;
        if (endMonth != null && !endMonth.isEmpty()) {
            cal = Calendar.getInstance();
            cal.setTime(DateUtil.parseDate(endMonth + "-01"));
        } else {
            cal = Calendar.getInstance();
        }

        String startMonth = DateUtil.format(DateUtil.offsetMonth(cal.getTime(), -(months - 1)), "yyyy-MM");
        String endMonthStr = DateUtil.format(cal.getTime(), "yyyy-MM");

        for (int i = months - 1; i >= 0; i--) {
            Calendar temp = (Calendar) cal.clone();
            temp.add(Calendar.MONTH, -i);
            monthList.add(DateUtil.format(temp.getTime(), "yyyy-MM"));
        }

        Date startDate = DateUtil.parseDate(startMonth + "-01");
        Date endDate = DateUtil.endOfMonth(DateUtil.parseDate(endMonthStr + "-01"));

        List<Attendance> allRecords = attendanceService.lambdaQuery()
                .ge(Attendance::getAttendanceDate, DateUtil.formatDate(startDate))
                .le(Attendance::getAttendanceDate, DateUtil.formatDate(endDate))
                .list();

        Map<Long, Employee> employeeMap = employeeService.lambdaQuery()
                .eq(Employee::getIsDeleted, 0).list().stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));

        Map<Long, List<Employee>> deptEmployeeMap = new HashMap<>();
        for (Employee e : employeeMap.values()) {
            if (e.getDepartmentId() != null) {
                deptEmployeeMap.computeIfAbsent(e.getDepartmentId(), k -> new ArrayList<>()).add(e);
            }
        }

        Map<String, Map<Long, List<Attendance>>> monthDeptRecordMap = new HashMap<>();
        for (Attendance r : allRecords) {
            Employee emp = employeeMap.get(r.getEmployeeId());
            if (emp != null && emp.getDepartmentId() != null) {
                String month = DateUtil.formatDate(r.getAttendanceDate()).substring(0, 7);
                monthDeptRecordMap.computeIfAbsent(month, k -> new HashMap<>())
                        .computeIfAbsent(emp.getDepartmentId(), k -> new ArrayList<>()).add(r);
            }
        }

        Map<String, Integer> monthWorkDaysMap = new HashMap<>();
        for (String month : monthList) {
            Date mStart = DateUtil.parseDate(month + "-01");
            Date mEnd = DateUtil.endOfMonth(mStart);
            monthWorkDaysMap.put(month, countWorkDays(mStart, mEnd));
        }

        for (String month : monthList) {
            Map<Long, List<Attendance>> deptRecordsMap = monthDeptRecordMap.getOrDefault(month, Collections.emptyMap());
            int workDays = monthWorkDaysMap.get(month);

            if (allDepts) {
                int totalAttendance = 0;
                int totalShould = 0;
                for (Map.Entry<Long, List<Attendance>> entry : deptRecordsMap.entrySet()) {
                    Long deptId = entry.getKey();
                    List<Attendance> records = entry.getValue();
                    int empCount = deptEmployeeMap.getOrDefault(deptId, Collections.emptyList()).size();
                    int attendanceDays = 0;
                    for (Attendance r : records) {
                        int status = r.getStatus() != null ? r.getStatus() : 0;
                        if (status == 0 || status == 1 || status == 2 || status == 9) {
                            attendanceDays++;
                        }
                    }
                    totalAttendance += attendanceDays;
                    totalShould += workDays * empCount;
                }
                double rate = totalShould > 0 ? (totalAttendance * 100.0 / totalShould) : 0;
                rateList.add(BigDecimal.valueOf(rate).setScale(2, RoundingMode.HALF_UP).doubleValue());
            } else {
                List<Attendance> records = deptRecordsMap.getOrDefault(departmentId, Collections.emptyList());
                int empCount = deptEmployeeMap.getOrDefault(departmentId, Collections.emptyList()).size();
                int attendanceDays = 0;
                for (Attendance r : records) {
                    int status = r.getStatus() != null ? r.getStatus() : 0;
                    if (status == 0 || status == 1 || status == 2 || status == 9) {
                        attendanceDays++;
                    }
                }
                double totalShould = workDays * empCount;
                double rate = totalShould > 0 ? (attendanceDays * 100.0 / totalShould) : 0;
                rateList.add(BigDecimal.valueOf(rate).setScale(2, RoundingMode.HALF_UP).doubleValue());
            }
        }

        vo.setMonths(monthList);
        vo.setRates(rateList);
        return vo;
    }

    @Override
    public LeaveTypeDistributionVO getLeaveTypeDistribution(String month, Long departmentId) {
        Date monthStart = DateUtil.parseDate(month + "-01");
        Date monthEnd = DateUtil.endOfMonth(monthStart);

        // 按部门过滤：先查出部门下的员工ID
        Set<Long> deptEmpIds = null;
        if (departmentId != null && departmentId > 0) {
            deptEmpIds = employeeService.lambdaQuery()
                    .eq(Employee::getDepartmentId, departmentId)
                    .eq(Employee::getIsDeleted, 0)
                    .list().stream().map(Employee::getId).collect(Collectors.toSet());
            if (deptEmpIds.isEmpty()) {
                return new LeaveTypeDistributionVO();
            }
        }

        List<Leave> leaves = leaveService.lambdaQuery()
                .eq(Leave::getStatus, 1)
                .ge(Leave::getStartDate, DateUtil.formatDate(monthStart))
                .le(Leave::getStartDate, DateUtil.formatDate(monthEnd))
                .in(deptEmpIds != null, Leave::getEmployeeId, deptEmpIds)
                .list();

        Map<Integer, Integer> typeCountMap = new HashMap<>();
        for (Leave leave : leaves) {
            typeCountMap.merge(leave.getLeaveType(), 1, Integer::sum);
        }

        List<String> leaveTypes = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        List<Double> percentages = new ArrayList<>();

        int total = typeCountMap.values().stream().mapToInt(Integer::intValue).sum();

        for (Integer type : typeCountMap.keySet()) {
            leaveTypes.add(getLeaveTypeText(type));
            int count = typeCountMap.get(type);
            counts.add(count);
            double percentage = total > 0 ? (count * 100.0 / total) : 0;
            percentages.add(BigDecimal.valueOf(percentage).setScale(2, RoundingMode.HALF_UP).doubleValue());
        }

        LeaveTypeDistributionVO vo = new LeaveTypeDistributionVO();
        vo.setLeaveTypes(leaveTypes);
        vo.setCounts(counts);
        vo.setPercentages(percentages);
        return vo;
    }

    @Override
    public List<AttendanceStatsVO> getLateEarlyRanking(String month, Long departmentId) {
        Date monthStart = DateUtil.parseDate(month + "-01");
        Date monthEnd = DateUtil.endOfMonth(monthStart);

        // 按部门过滤
        Set<Long> deptEmpIds = null;
        if (departmentId != null && departmentId > 0) {
            deptEmpIds = employeeService.lambdaQuery()
                    .eq(Employee::getDepartmentId, departmentId)
                    .eq(Employee::getIsDeleted, 0)
                    .list().stream().map(Employee::getId).collect(Collectors.toSet());
            if (deptEmpIds.isEmpty()) {
                return Collections.emptyList();
            }
        }

        List<Attendance> records = attendanceService.lambdaQuery()
                .ge(Attendance::getAttendanceDate, DateUtil.formatDate(monthStart))
                .le(Attendance::getAttendanceDate, DateUtil.formatDate(monthEnd))
                .in(Attendance::getStatus, Arrays.asList(1, 2))
                .in(deptEmpIds != null, Attendance::getEmployeeId, deptEmpIds)
                .list();

        Map<Long, Integer> lateCountMap = new HashMap<>();
        Map<Long, Integer> earlyCountMap = new HashMap<>();
        for (Attendance r : records) {
            if (r.getStatus() == 1) {
                lateCountMap.merge(r.getEmployeeId(), 1, Integer::sum);
            } else if (r.getStatus() == 2) {
                earlyCountMap.merge(r.getEmployeeId(), 1, Integer::sum);
            }
        }

        Set<Long> empIds = new HashSet<>();
        empIds.addAll(lateCountMap.keySet());
        empIds.addAll(earlyCountMap.keySet());

        List<Employee> employees = employeeMapper.selectBatchIdsAll(new ArrayList<>(empIds));
        Map<Long, Employee> empMap = employees.stream().collect(Collectors.toMap(Employee::getId, e -> e));

        List<AttendanceStatsVO> result = new ArrayList<>();
        for (Long empId : empIds) {
            Employee emp = empMap.get(empId);
            if (emp == null) continue;

            AttendanceStatsVO vo = new AttendanceStatsVO();
            vo.setEmployeeId(empId);
            vo.setEmployeeName(emp.getEmployeeName());
            vo.setLateDays(lateCountMap.getOrDefault(empId, 0));
            vo.setEarlyDays(earlyCountMap.getOrDefault(empId, 0));

            if (emp.getDepartmentId() != null) {
                Department dept = departmentService.getById(emp.getDepartmentId());
                vo.setDepartmentName(dept != null ? dept.getDeptName() : null);
            }

            result.add(vo);
        }

        result.sort((a, b) -> {
            int compareLate = Integer.compare(b.getLateDays(), a.getLateDays());
            if (compareLate != 0) return compareLate;
            return Integer.compare(b.getEarlyDays(), a.getEarlyDays());
        });

        return result;
    }

    @Override
    public AttendanceTrendVO getPersonalTrend(Long userId, Integer months) {
        AttendanceTrendVO vo = new AttendanceTrendVO();
        List<String> monthList = new ArrayList<>();
        List<Double> rateList = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        for (int i = months - 1; i >= 0; i--) {
            Calendar temp = (Calendar) cal.clone();
            temp.add(Calendar.MONTH, -i);
            monthList.add(DateUtil.format(temp.getTime(), "yyyy-MM"));
        }

        String startMonth = monthList.get(0);
        String endMonthStr = monthList.get(monthList.size() - 1);

        Date startDate = DateUtil.parseDate(startMonth + "-01");
        Date endDate = DateUtil.endOfMonth(DateUtil.parseDate(endMonthStr + "-01"));

        Employee emp = employeeService.getByUserId(userId);
        if (emp == null) {
            vo.setMonths(monthList);
            vo.setRates(Collections.nCopies(months, 0.0));
            return vo;
        }

        List<Attendance> allRecords = attendanceService.lambdaQuery()
                .eq(Attendance::getEmployeeId, emp.getId())
                .ge(Attendance::getAttendanceDate, DateUtil.formatDate(startDate))
                .le(Attendance::getAttendanceDate, DateUtil.formatDate(endDate))
                .list();

        Map<String, int[]> monthStatsMap = new HashMap<>();
        for (String month : monthList) {
            monthStatsMap.put(month, new int[]{0, 0, 0, 0, 0, 0});
        }

        for (Attendance r : allRecords) {
            String month = DateUtil.formatDate(r.getAttendanceDate()).substring(0, 7);
            int[] stats = monthStatsMap.get(month);
            if (stats != null) {
                int status = r.getStatus() != null ? r.getStatus() : 0;
                switch (status) {
                    case 0: stats[0]++; break;
                    case 1: case 9: stats[1]++; break;
                    case 2: stats[2]++; break;
                    case 3: case 6: case 7: stats[3]++; break;
                    case 4: stats[4]++; break;
                    case 5: stats[5]++; break;
                    default: break;
                }
            }
        }

        for (String month : monthList) {
            Date mStart = DateUtil.parseDate(month + "-01");
            Date mEnd = DateUtil.endOfMonth(mStart);
            int totalDays = countWorkDays(mStart, mEnd);

            int[] stats = monthStatsMap.get(month);
            int actualDays = stats[0] + stats[1] + stats[2];
            double rate = totalDays > 0 ? (actualDays * 100.0 / totalDays) : 0;
            rateList.add(BigDecimal.valueOf(rate).setScale(2, RoundingMode.HALF_UP).doubleValue());
        }

        vo.setMonths(monthList);
        vo.setRates(rateList);
        return vo;
    }

    @Override
    public LeaveTypeDistributionVO getPersonalLeaveDistribution(Long userId, String month) {
        Employee emp = employeeService.getByUserId(userId);
        if (emp == null) return new LeaveTypeDistributionVO();

        Date monthStart = DateUtil.parseDate(month + "-01");
        Date monthEnd = DateUtil.endOfMonth(monthStart);

        List<Leave> leaves = leaveService.lambdaQuery()
                .eq(Leave::getEmployeeId, emp.getId())
                .eq(Leave::getStatus, 1)
                .ge(Leave::getStartDate, DateUtil.formatDate(monthStart))
                .le(Leave::getStartDate, DateUtil.formatDate(monthEnd))
                .list();

        Map<Integer, Integer> typeCountMap = new HashMap<>();
        for (Leave leave : leaves) {
            typeCountMap.merge(leave.getLeaveType(), 1, Integer::sum);
        }

        List<String> leaveTypes = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        List<Double> percentages = new ArrayList<>();
        int total = typeCountMap.values().stream().mapToInt(Integer::intValue).sum();

        for (Integer type : typeCountMap.keySet()) {
            leaveTypes.add(getLeaveTypeText(type));
            int count = typeCountMap.get(type);
            counts.add(count);
            double percentage = total > 0 ? (count * 100.0 / total) : 0;
            percentages.add(BigDecimal.valueOf(percentage).setScale(2, RoundingMode.HALF_UP).doubleValue());
        }

        LeaveTypeDistributionVO vo = new LeaveTypeDistributionVO();
        vo.setLeaveTypes(leaveTypes);
        vo.setCounts(counts);
        vo.setPercentages(percentages);
        return vo;
    }

    private int countWorkDays(Date start, Date end) {
        List<HolidayConfig> configs = holidayConfigService.lambdaQuery()
                .ge(HolidayConfig::getHolidayDate, DateUtil.formatDate(start))
                .le(HolidayConfig::getHolidayDate, DateUtil.formatDate(end))
                .list();
        Set<String> holidaySet = new HashSet<>();
        Set<String> specialWorkSet = new HashSet<>();
        for (HolidayConfig c : configs) {
            String d = DateUtil.formatDate(c.getHolidayDate());
            if (c.getHolidayType() != null && c.getHolidayType() == 1) {
                specialWorkSet.add(d);
            } else {
                holidaySet.add(d);
            }
        }
        int count = 0;
        Calendar cursor = Calendar.getInstance();
        cursor.setTime(start);
        while (!cursor.getTime().after(end)) {
            String dateStr = DateUtil.formatDate(cursor.getTime());
            int dayOfWeek = cursor.get(Calendar.DAY_OF_WEEK);
            boolean isWeekend = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY);
            if (specialWorkSet.contains(dateStr) || (!isWeekend && !holidaySet.contains(dateStr))) {
                count++;
            }
            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }
        return count;
    }

    private String getLeaveTypeText(Integer leaveType) {
        if (leaveType == null) return "其他";
        switch (leaveType) {
            case 0: return "事假";
            case 1: return "病假";
            case 2: return "年假";
            case 3: return "婚假";
            case 4: return "产假";
            case 5: return "丧假";
            case 6: return "调休";
            default: return "其他";
        }
    }
}
