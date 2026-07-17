package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.DataScopeContext;
import com.limou.hrms.constant.DataScopeHelper;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.*;
import com.limou.hrms.model.dto.attendance.AttendanceRecordQueryRequest;
import com.limou.hrms.model.dto.attendance.ClockRequest;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.vo.AttendanceCalendarVO;
import com.limou.hrms.model.vo.AttendanceCalendarVO.DayItem;
import com.limou.hrms.model.vo.AttendanceCalendarVO.Summary;
import com.limou.hrms.model.vo.AttendanceRecordVO;
import com.limou.hrms.model.vo.ClockResultVO;
import com.limou.hrms.service.AttendanceService;
import com.limou.hrms.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 打卡服务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRecordMapper attendanceRecordMapper;
    private final AttendanceGroupRuleMapper attendanceGroupRuleMapper;
    private final AttendanceGroupMapper attendanceGroupMapper;
    private final EmployeeWorkInfoMapper employeeWorkInfoMapper;
    private final EmployeePersonalInfoMapper employeePersonalInfoMapper;
    private final LeaveRequestMapper leaveRequestMapper;
    private final DataScopeContext dataScopeContext;
    private final DataScopeHelper dataScopeHelper;
    private final DepartmentService departmentService;
    private final WorkCalendarMapper workCalendarMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClockResultVO clock(ClockRequest dto) {
        Long employeeId = dataScopeContext.getCurrentEmployeeId();
        if (employeeId == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        int clockType = dto.getClockType();
        if (clockType != 1 && clockType != 2) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的打卡类型");
        }

        // ① 匹配考勤组
        AttendanceGroup group = matchAttendanceGroup(employeeId);
        if (group == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未匹配到任何考勤组，请联系HR配置");
        }

        // ② IP 白名单校验（预留，后续接入 HttpServletRequest 获取 IP）
        // String clientIp = getClientIp();
        // if (StringUtils.hasText(group.getIpWhitelist())) {
        //     validateIpWhitelist(clientIp, group.getIpWhitelist());
        // }

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // ③ 请假联动
        checkLeaveConflict(employeeId, today, now, clockType);

        // ④ 拿到或创建今天的打卡记录
        AttendanceRecord record = getOrCreateRecord(employeeId, today, group);

        ClockResultVO result = new ClockResultVO();
        result.setAttendanceDate(today);
        result.setClockType(clockType);

        if (clockType == 1) {
            // 上班打卡
            if (record.getActualStartTime() != null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "已完成上班打卡，无需重复打卡");
            }
            record.setActualStartTime(now);
            int status = judgeStartStatus(now, group.getStartTime(), group.getLateThreshold());
            record.setStartStatus(status);

            result.setActualTime(now);
            result.setStatus(status);
            result.setStatusDesc(getStartStatusDesc(status));
            result.setScheduledTime(group.getStartTime().toString());
        } else {
            // 下班打卡
            if (record.getActualEndTime() != null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "已完成下班打卡，无需重复打卡");
            }
            record.setActualEndTime(now);
            int status = judgeEndStatus(now, group.getEndTime(), group.getEarlyLeaveThreshold());
            record.setEndStatus(status);

            result.setActualTime(now);
            result.setStatus(status);
            result.setStatusDesc(getEndStatusDesc(status));
            result.setScheduledTime(group.getEndTime().toString());
        }

        result.setClockTypeDesc(clockType == 1 ? "上班打卡" : "下班打卡");

        attendanceRecordMapper.updateById(record);

        log.info("员工 {} 完成了{}，状态={}", employeeId, result.getClockTypeDesc(), result.getStatusDesc());
        return result;
    }

    // ==================== 查询列表 ====================

    @Override
    public Page<AttendanceRecordVO> queryRecords(AttendanceRecordQueryRequest queryReq) {
        QueryWrapper<AttendanceRecord> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("attendance_date", "id");

        // 数据权限过滤
        dataScopeHelper.applyAttendanceScope(wrapper);

        // 额外筛选条件
        if (queryReq.getEmployeeId() != null) {
            wrapper.eq("employee_id", queryReq.getEmployeeId());
        }
        if (queryReq.getDepartmentId() != null) {
            wrapper.inSql("employee_id",
                    "SELECT e.id FROM employee e " +
                    "INNER JOIN employee_work_info ewi ON e.id = ewi.employee_id " +
                    "WHERE ewi.department_id = " + queryReq.getDepartmentId());
        }
        if (queryReq.getStartDate() != null) {
            wrapper.ge("attendance_date", queryReq.getStartDate());
        }
        if (queryReq.getEndDate() != null) {
            wrapper.le("attendance_date", queryReq.getEndDate());
        }
        if (queryReq.getStartStatus() != null) {
            wrapper.eq("start_status", queryReq.getStartStatus());
        }
        if (queryReq.getEndStatus() != null) {
            wrapper.eq("end_status", queryReq.getEndStatus());
        }

        Page<AttendanceRecord> page = new Page<>(queryReq.getCurrent(), queryReq.getPageSize());
        attendanceRecordMapper.selectPage(page, wrapper);

        // 批量查 employeeName、departmentName
        Set<Long> empIds = page.getRecords().stream()
                .map(AttendanceRecord::getEmployeeId).collect(Collectors.toSet());
        Map<Long, String> empNameMap = loadEmpNameMap(empIds);
        Map<Long, String> deptNameMap = loadDeptNameMap(empIds);

        List<AttendanceRecordVO> voList = page.getRecords().stream().map(r -> {
            AttendanceRecordVO vo = new AttendanceRecordVO();
            BeanUtils.copyProperties(r, vo);
            if (r.getScheduledStartTime() != null) vo.setScheduledStartTime(r.getScheduledStartTime().toString());
            if (r.getScheduledEndTime() != null) vo.setScheduledEndTime(r.getScheduledEndTime().toString());
            vo.setEmployeeName(empNameMap.getOrDefault(r.getEmployeeId(), ""));
            vo.setDepartmentName(deptNameMap.getOrDefault(r.getEmployeeId(), ""));
            vo.setStartStatusDesc(getStartStatusDesc(r.getStartStatus()));
            vo.setEndStatusDesc(getEndStatusDesc(r.getEndStatus()));
            return vo;
        }).collect(Collectors.toList());

        Page<AttendanceRecordVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(voList);
        return result;
    }

    private Map<Long, String> loadEmpNameMap(Set<Long> empIds) {
        if (empIds.isEmpty()) return Collections.emptyMap();
        return employeePersonalInfoMapper.selectList(
                        Wrappers.<EmployeePersonalInfo>lambdaQuery()
                                .in(EmployeePersonalInfo::getEmployeeId, empIds))
                .stream()
                .collect(Collectors.toMap(EmployeePersonalInfo::getEmployeeId, EmployeePersonalInfo::getName));
    }

    private Map<Long, String> loadDeptNameMap(Set<Long> empIds) {
        if (empIds.isEmpty()) return Collections.emptyMap();
        List<EmployeeWorkInfo> workInfos = employeeWorkInfoMapper.selectList(
                Wrappers.<EmployeeWorkInfo>lambdaQuery()
                        .in(EmployeeWorkInfo::getEmployeeId, empIds));
        Set<Long> deptIds = workInfos.stream()
                .map(EmployeeWorkInfo::getDepartmentId).collect(Collectors.toSet());
        Map<Long, String> deptNameMap = departmentService.listByIds(new ArrayList<>(deptIds)).stream()
                .collect(Collectors.toMap(Department::getId, Department::getName));
        Map<Long, Long> empDeptMap = workInfos.stream()
                .collect(Collectors.toMap(EmployeeWorkInfo::getEmployeeId, EmployeeWorkInfo::getDepartmentId, (a, b) -> a));
        return empIds.stream()
                .collect(Collectors.toMap(id -> id, id -> deptNameMap.getOrDefault(empDeptMap.get(id), "")));
    }

    // ==================== 日历视图 ====================

    @Override
    public AttendanceCalendarVO getCalendar(int year, int month, Long employeeId) {
        if (employeeId == null) {
            employeeId = dataScopeContext.getCurrentEmployeeId();
        }

        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.plusMonths(1).minusDays(1);

        // 查工作日历
        List<WorkCalendar> calendars = workCalendarMapper.selectList(
                Wrappers.<WorkCalendar>lambdaQuery()
                        .between(WorkCalendar::getCalendarDate, firstDay, lastDay));
        Map<LocalDate, WorkCalendar> calendarMap = calendars.stream()
                .collect(Collectors.toMap(WorkCalendar::getCalendarDate, c -> c));

        // 查打卡记录
        List<AttendanceRecord> records = attendanceRecordMapper.selectList(
                Wrappers.<AttendanceRecord>lambdaQuery()
                        .eq(AttendanceRecord::getEmployeeId, employeeId)
                        .between(AttendanceRecord::getAttendanceDate, firstDay, lastDay));
        Map<LocalDate, AttendanceRecord> recordMap = records.stream()
                .collect(Collectors.toMap(AttendanceRecord::getAttendanceDate, r -> r, (a, b) -> a));

        // 查请假记录
        List<LeaveRequest> leaves = leaveRequestMapper.selectList(
                Wrappers.<LeaveRequest>lambdaQuery()
                        .eq(LeaveRequest::getEmployeeId, employeeId)
                        .eq(LeaveRequest::getStatus, 3)
                        .lt(LeaveRequest::getStartTime, lastDay.plusDays(1).atStartOfDay())
                        .gt(LeaveRequest::getEndTime, firstDay.atStartOfDay()));
        Set<LocalDate> leaveDates = new HashSet<>();
        for (LeaveRequest lv : leaves) {
            LocalDate start = lv.getStartTime().toLocalDate();
            LocalDate end = lv.getEndTime().toLocalDate();
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                leaveDates.add(d);
            }
        }

        // 构建每天数据 + summary
        int normalDays = 0, lateDays = 0, earlyLeaveDays = 0, absentDays = 0, cardMissingDays = 0, leaveDays = 0;
        List<DayItem> days = new ArrayList<>();

        for (LocalDate d = firstDay; !d.isAfter(lastDay); d = d.plusDays(1)) {
            DayItem item = new DayItem();
            item.setDate(d.toString());
            item.setHasLeave(leaveDates.contains(d));

            WorkCalendar cal = calendarMap.get(d);
            if (cal != null) {
                item.setDayType(cal.getDayType());
                item.setDayTypeDesc(getDayTypeDesc(cal.getDayType()));
            }

            AttendanceRecord record = recordMap.get(d);
            if (record != null) {
                item.setStartStatus(record.getStartStatus());
                item.setStartStatusDesc(getStartStatusDesc(record.getStartStatus()));
                item.setEndStatus(record.getEndStatus());
                item.setEndStatusDesc(getEndStatusDesc(record.getEndStatus()));

                if (isNormal(record.getStartStatus())) normalDays++;
                if (record.getStartStatus() != null && record.getStartStatus() == 2) lateDays++;
                if (record.getEndStatus() != null && record.getEndStatus() == 2) earlyLeaveDays++;
                if (isAbsent(record.getStartStatus())) absentDays++;
                if (isAbsent(record.getEndStatus())) absentDays++;
                if (isMissing(record.getStartStatus())) cardMissingDays++;
                if (isMissing(record.getEndStatus())) cardMissingDays++;
            }
            if (item.getHasLeave()) leaveDays++;

            days.add(item);
        }

        Summary sum = new Summary();
        sum.setNormalDays(normalDays);
        sum.setLateDays(lateDays);
        sum.setEarlyLeaveDays(earlyLeaveDays);
        sum.setAbsentDays(absentDays);
        sum.setCardMissingDays(cardMissingDays);
        sum.setLeaveDays(leaveDays);

        AttendanceCalendarVO vo = new AttendanceCalendarVO();
        vo.setYear(year);
        vo.setMonth(month);
        vo.setDays(days);
        vo.setSummary(sum);
        return vo;
    }

    private boolean isNormal(Integer status) {
        return status != null && status == 1;
    }

    private boolean isAbsent(Integer status) {
        return status != null && status == 3;
    }

    private boolean isMissing(Integer status) {
        return status != null && status == 4;
    }

    private String getDayTypeDesc(Integer dayType) {
        if (dayType == null) return null;
        switch (dayType) {
            case 1: return "工作日";
            case 2: return "休息日";
            case 3: return "节假日";
            default: return "未知";
        }
    }

    // ==================== 考勤组匹配 ====================

    /**
     * 按优先级匹配考勤组：个人(3) → 职位(2) → 部门(1)
     */
    private AttendanceGroup matchAttendanceGroup(Long employeeId) {
        EmployeeWorkInfo workInfo = employeeWorkInfoMapper.selectOne(
                Wrappers.<EmployeeWorkInfo>lambdaQuery()
                        .eq(EmployeeWorkInfo::getEmployeeId, employeeId));
        if (workInfo == null) {
            return null;
        }

        List<AttendanceGroupRule> allRules = attendanceGroupRuleMapper.selectList(
                Wrappers.<AttendanceGroupRule>lambdaQuery()
                        .in(AttendanceGroupRule::getRuleType, 3, 2, 1)
                        .orderByAsc(AttendanceGroupRule::getRuleType));

        // 按个人(3)
        for (AttendanceGroupRule rule : allRules) {
            if (rule.getRuleType() == 3 && rule.getTargetId().equals(employeeId)) {
                return attendanceGroupMapper.selectById(rule.getAttendanceGroupId());
            }
        }
        // 按职位(2)
        for (AttendanceGroupRule rule : allRules) {
            if (rule.getRuleType() == 2 && rule.getTargetId().equals(workInfo.getPositionId())) {
                return attendanceGroupMapper.selectById(rule.getAttendanceGroupId());
            }
        }
        // 按部门(1)
        for (AttendanceGroupRule rule : allRules) {
            if (rule.getRuleType() == 1 && rule.getTargetId().equals(workInfo.getDepartmentId())) {
                return attendanceGroupMapper.selectById(rule.getAttendanceGroupId());
            }
        }
        return null;
    }

    // ==================== 请假联动 ====================

    /**
     * 检查当天是否有已通过的请假记录，冲突则拒绝
     */
    private void checkLeaveConflict(Long employeeId, LocalDate today, LocalDateTime now, int clockType) {
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);

        List<LeaveRequest> leaves = leaveRequestMapper.selectList(
                Wrappers.<LeaveRequest>lambdaQuery()
                        .eq(LeaveRequest::getEmployeeId, employeeId)
                        .eq(LeaveRequest::getStatus, 3)  // 已通过
                        .lt(LeaveRequest::getStartTime, todayEnd)
                        .gt(LeaveRequest::getEndTime, todayStart));

        for (LeaveRequest leave : leaves) {
            // 全天请假（leaveDays >= 1.0 + 覆盖全天）→ 拒绝
            if (leave.getLeaveDays() != null && leave.getLeaveDays().compareTo(new java.math.BigDecimal("1.0")) >= 0
                    && !leave.getStartTime().isAfter(todayStart)
                    && !leave.getEndTime().isBefore(todayEnd)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "当日已请假，无需打卡");
            }
            // 半天请假：校验当前时段
            if (clockType == 1 && now.isBefore(leave.getEndTime()) && now.isAfter(leave.getStartTime())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前时段已请假，无需打卡");
            }
            if (clockType == 2 && now.isBefore(leave.getEndTime()) && now.isAfter(leave.getStartTime())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "当前时段已请假，无需打卡");
            }
        }
    }

    // ==================== 打卡记录 ====================

    /**
     * 获取或创建今天的打卡记录
     */
    private AttendanceRecord getOrCreateRecord(Long employeeId, LocalDate today, AttendanceGroup group) {
        AttendanceRecord record = attendanceRecordMapper.selectOne(
                Wrappers.<AttendanceRecord>lambdaQuery()
                        .eq(AttendanceRecord::getEmployeeId, employeeId)
                        .eq(AttendanceRecord::getAttendanceDate, today));
        if (record != null) {
            return record;
        }

        record = new AttendanceRecord();
        record.setEmployeeId(employeeId);
        record.setAttendanceDate(today);
        record.setAttendanceGroupId(group.getId());
        record.setScheduledStartTime(group.getStartTime());
        record.setScheduledEndTime(group.getEndTime());
        attendanceRecordMapper.insert(record);
        return record;
    }

    // ==================== 状态判定 ====================

    /**
     * 上班卡状态判定
     */
    private int judgeStartStatus(LocalDateTime actualTime, LocalTime scheduledTime, Integer lateThreshold) {
        if (lateThreshold == null) lateThreshold = 15;
        LocalTime actual = actualTime.toLocalTime();
        LocalTime lateLine = scheduledTime.plusMinutes(lateThreshold);

        if (!actual.isAfter(scheduledTime)) {
            return 1; // 正常
        } else if (!actual.isAfter(lateLine)) {
            return 2; // 迟到
        } else {
            return 3; // 旷工半天
        }
    }

    /**
     * 下班卡状态判定
     */
    private int judgeEndStatus(LocalDateTime actualTime, LocalTime scheduledTime, Integer earlyLeaveThreshold) {
        if (earlyLeaveThreshold == null) earlyLeaveThreshold = 15;
        LocalTime actual = actualTime.toLocalTime();
        LocalTime earlyLine = scheduledTime.minusMinutes(earlyLeaveThreshold);

        if (!actual.isBefore(scheduledTime)) {
            return 1; // 正常
        } else if (!actual.isBefore(earlyLine)) {
            return 2; // 早退
        } else {
            return 3; // 旷工半天
        }
    }

    private String getStartStatusDesc(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 1: return "正常";
            case 2: return "迟到";
            case 3: return "旷工半天";
            case 4: return "缺卡";
            default: return "未知";
        }
    }

    private String getEndStatusDesc(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 1: return "正常";
            case 2: return "早退";
            case 3: return "旷工半天";
            case 4: return "缺卡";
            default: return "未知";
        }
    }
}
