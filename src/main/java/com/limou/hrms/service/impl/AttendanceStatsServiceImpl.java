package com.limou.hrms.service.impl;

import cn.hutool.core.date.DateUtil;
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
    private HolidayConfigService holidayConfigService;

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
            switch (r.getStatus()) {
                case 0: normalDays++; break;
                case 1: lateDays++; break;
                case 2: earlyDays++; break;
                case 3: missingDays++; break;
                case 4: leaveDays++; break;
                case 5: absentDays++; break;
            }
        }

        vo.setNormalDays(normalDays);
        vo.setLateDays(lateDays);
        vo.setEarlyDays(earlyDays);
        vo.setMissingDays(missingDays);
        vo.setLeaveDays(leaveDays);
        vo.setAbsentDays(absentDays);

        int actualDays = normalDays + lateDays + earlyDays;
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

        List<Department> departments = departmentService.lambdaQuery()
                .eq(Department::getIsDeleted, 0)
                .list();

        List<Attendance> allRecords = attendanceService.lambdaQuery()
                .ge(Attendance::getAttendanceDate, DateUtil.formatDate(monthStart))
                .le(Attendance::getAttendanceDate, DateUtil.formatDate(monthEnd))
                .list();

        Map<Long, Employee> employeeMap = employeeService.lambdaQuery()
                .eq(Employee::getIsDeleted, 0).list().stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));

        Map<Long, List<Employee>> deptEmployeeMap = employeeMap.values().stream()
                .filter(e -> e.getDepartmentId() != null)
                .collect(Collectors.groupingBy(Employee::getDepartmentId));

        // 按部门分组考勤记录
        Map<Long, List<Attendance>> deptRecordMap = new HashMap<>();
        for (Attendance r : allRecords) {
            Employee emp = employeeMap.get(r.getEmployeeId());
            if (emp != null && emp.getDepartmentId() != null) {
                deptRecordMap.computeIfAbsent(emp.getDepartmentId(), k -> new ArrayList<>()).add(r);
            }
        }

        int totalWorkDays = countWorkDays(monthStart, monthEnd);

        List<DepartmentAttendanceStatsVO> result = new ArrayList<>();
        for (Department dept : departments) {
            DepartmentAttendanceStatsVO vo = new DepartmentAttendanceStatsVO();
            vo.setDepartmentId(dept.getId());
            vo.setDepartmentName(dept.getDeptName());

            List<Employee> deptEmployees = deptEmployeeMap.getOrDefault(dept.getId(), Collections.emptyList());
            int empCount = deptEmployees.size();
            vo.setEmployeeCount(empCount);
            vo.setTotalWorkDays(totalWorkDays);

            List<Attendance> deptRecords = deptRecordMap.getOrDefault(dept.getId(), Collections.emptyList());

            // 统计出勤天数：正常+迟到+早退（都属于出勤）
            int attendanceDays = 0;
            int lateTotalTimes = 0;
            int earlyTotalTimes = 0;
            int absentDays = 0;
            int leaveDays = 0;
            // 用 Set 统计去重后的迟到/早退/请假人数
            Set<Long> lateEmployeeSet = new HashSet<>();
            Set<Long> earlyEmployeeSet = new HashSet<>();
            Set<Long> leaveEmployeeSet = new HashSet<>();

            for (Attendance r : deptRecords) {
                int status = r.getStatus() != null ? r.getStatus() : 0;
                if (status == AttendanceStatusEnum.NORMAL.getValue()
                        || status == AttendanceStatusEnum.LATE.getValue()
                        || status == AttendanceStatusEnum.LEAVE_EARLY.getValue()) {
                    attendanceDays++;
                }
                if (status == AttendanceStatusEnum.LATE.getValue()) {
                    lateTotalTimes++;
                    lateEmployeeSet.add(r.getEmployeeId());
                }
                if (status == AttendanceStatusEnum.LEAVE_EARLY.getValue()) {
                    earlyTotalTimes++;
                    earlyEmployeeSet.add(r.getEmployeeId());
                }
                if (status == AttendanceStatusEnum.LEAVE.getValue()) {
                    leaveDays++;
                    leaveEmployeeSet.add(r.getEmployeeId());
                }
                if (status == AttendanceStatusEnum.ABSENT.getValue()) {
                    absentDays++;
                }
            }

            vo.setActualAttendanceDays(attendanceDays);
            vo.setLateCount(lateTotalTimes);
            vo.setEarlyCount(earlyTotalTimes);
            vo.setAbsentDays(absentDays);
            vo.setLeaveDays(leaveDays);

            // 出勤率 = 实际出勤天数 / 应出勤总天数
            double totalShouldDays = totalWorkDays * empCount;
            double attendanceRate = totalShouldDays > 0
                    ? (attendanceDays * 100.0 / totalShouldDays) : 0;
            vo.setAttendanceRate(BigDecimal.valueOf(attendanceRate).setScale(2, RoundingMode.HALF_UP).doubleValue());

            // 迟到率 = 迟到人数 / 部门总人数
            double lateRate = empCount > 0
                    ? (lateEmployeeSet.size() * 100.0 / empCount) : 0;
            vo.setLateRate(BigDecimal.valueOf(lateRate).setScale(2, RoundingMode.HALF_UP).doubleValue());

            // 请假率 = 请假人数 / 部门总人数
            double leaveRate = empCount > 0
                    ? (leaveEmployeeSet.size() * 100.0 / empCount) : 0;
            vo.setLeaveRate(BigDecimal.valueOf(leaveRate).setScale(2, RoundingMode.HALF_UP).doubleValue());

            result.add(vo);
        }

        return result;
    }

    @Override
    public AttendanceTrendVO getAttendanceTrend(Long departmentId, Integer months) {
        AttendanceTrendVO vo = new AttendanceTrendVO();
        List<String> monthList = new ArrayList<>();
        List<Double> rateList = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        for (int i = months - 1; i >= 0; i--) {
            Calendar temp = (Calendar) cal.clone();
            temp.add(Calendar.MONTH, -i);
            String month = DateUtil.format(temp.getTime(), "yyyy-MM");
            monthList.add(month);

            List<DepartmentAttendanceStatsVO> statsList = getDepartmentStats(month);
            DepartmentAttendanceStatsVO stats = statsList.stream()
                    .filter(s -> s.getDepartmentId().equals(departmentId))
                    .findFirst().orElse(null);

            rateList.add(stats != null ? stats.getAttendanceRate() : 0.0);
        }

        vo.setMonths(monthList);
        vo.setRates(rateList);
        return vo;
    }

    @Override
    public LeaveTypeDistributionVO getLeaveTypeDistribution(String month) {
        Date monthStart = DateUtil.parseDate(month + "-01");
        Date monthEnd = DateUtil.endOfMonth(monthStart);

        List<Leave> leaves = leaveService.lambdaQuery()
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

    @Override
    public List<AttendanceStatsVO> getLateEarlyRanking(String month) {
        Date monthStart = DateUtil.parseDate(month + "-01");
        Date monthEnd = DateUtil.endOfMonth(monthStart);

        List<Attendance> records = attendanceService.lambdaQuery()
                .ge(Attendance::getAttendanceDate, DateUtil.formatDate(monthStart))
                .le(Attendance::getAttendanceDate, DateUtil.formatDate(monthEnd))
                .in(Attendance::getStatus, Arrays.asList(1, 2))
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

        List<Employee> employees = employeeService.listByIds(new ArrayList<>(empIds));
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
            String month = DateUtil.format(temp.getTime(), "yyyy-MM");
            monthList.add(month);
            AttendanceStatsVO stats = getPersonalStats(userId, month);
            rateList.add(stats != null ? stats.getAttendanceRate() : 0.0);
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
        int count = 0;
        Calendar cursor = Calendar.getInstance();
        cursor.setTime(start);
        while (!cursor.getTime().after(end)) {
            if (holidayConfigService.isWorkDay(cursor.getTime())) {
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
