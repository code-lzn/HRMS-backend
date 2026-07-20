package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.DataScopeContext;
import com.limou.hrms.constant.DataScopeEnum;
import com.limou.hrms.constant.DataScopeHelper;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.*;
import com.limou.hrms.model.dto.attendance.AttendanceRecordQueryRequest;
import com.limou.hrms.model.dto.attendance.ClockRequest;
import com.limou.hrms.model.dto.attendance.SupplementCardSubmitDTO;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.vo.AttendanceCalendarVO;
import com.limou.hrms.model.vo.AttendanceCalendarVO.DayItem;
import com.limou.hrms.model.vo.AttendanceCalendarVO.Summary;
import com.limou.hrms.model.vo.AttendanceRecordVO;
import com.limou.hrms.model.vo.ClockResultVO;
import com.limou.hrms.model.vo.SupplementCardListVO;
import com.limou.hrms.model.vo.SupplementCardVO;
import com.limou.hrms.service.ApprovalCallback;
import com.limou.hrms.service.ApprovalFlowService;
import com.limou.hrms.service.AttendanceService;
import com.limou.hrms.service.DepartmentService;
import com.limou.hrms.model.enums.ApprovalBizType;
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
public class AttendanceServiceImpl implements AttendanceService, ApprovalCallback {

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
    private final SupplementCardRequestMapper supplementCardRequestMapper;
    private final ApprovalFlowService approvalFlowService;

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

        boolean isFlex = group.getShiftType() != null && group.getShiftType() == 2;

        if (clockType == 1) {
            // 上班打卡
            if (record.getActualStartTime() != null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "已完成上班打卡，无需重复打卡");
            }
            record.setActualStartTime(now);
            int status;
            if (isFlex) {
                status = judgeFlexStartStatus(now, group.getFlexStartTime(), group.getFlexEndTime(),
                        group.getLateThreshold());
            } else {
                status = judgeStartStatus(now, group.getStartTime(), group.getLateThreshold());
            }
            record.setStartStatus(status);

            result.setActualTime(now);
            result.setStatus(status);
            result.setStatusDesc(getStartStatusDesc(status));
            result.setScheduledTime(isFlex ? group.getFlexStartTime().toString() : group.getStartTime().toString());
        } else {
            // 下班打卡
            if (record.getActualEndTime() != null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "已完成下班打卡，无需重复打卡");
            }
            record.setActualEndTime(now);
            int status;
            if (isFlex) {
                LocalDateTime startTime = record.getActualStartTime();
                if (startTime == null) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "未找到上班打卡记录");
                }
                java.math.BigDecimal workHours = group.getWorkHours() != null ? group.getWorkHours() : new java.math.BigDecimal("8");
                status = judgeFlexEndStatus(startTime, now, workHours, group.getEarlyLeaveThreshold());
            } else {
                status = judgeEndStatus(now, group.getEndTime(), group.getEarlyLeaveThreshold());
            }
            record.setEndStatus(status);

            result.setActualTime(now);
            result.setStatus(status);
            result.setStatusDesc(getEndStatusDesc(status));
            result.setScheduledTime(isFlex ? group.getFlexEndTime().toString() : group.getEndTime().toString());
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

        // 批量查考勤组（用于弹性班展示核心时间）
        Set<Long> groupIds = page.getRecords().stream()
                .map(AttendanceRecord::getAttendanceGroupId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, AttendanceGroup> groupMap = loadGroupMap(groupIds);

        List<AttendanceRecordVO> voList = page.getRecords().stream().map(r -> {
            AttendanceRecordVO vo = new AttendanceRecordVO();
            BeanUtils.copyProperties(r, vo);

            AttendanceGroup group = groupMap.get(r.getAttendanceGroupId());
            boolean isFlex = group != null && group.getShiftType() != null && group.getShiftType() == 2;
            vo.setShiftType(group != null ? group.getShiftType() : null);

            if (isFlex && group.getCoreStartTime() != null) {
                vo.setScheduledStartTime(group.getCoreStartTime().toString());
            } else if (r.getScheduledStartTime() != null) {
                vo.setScheduledStartTime(r.getScheduledStartTime().toString());
            }
            if (isFlex && group.getCoreEndTime() != null) {
                vo.setScheduledEndTime(group.getCoreEndTime().toString());
            } else if (r.getScheduledEndTime() != null) {
                vo.setScheduledEndTime(r.getScheduledEndTime().toString());
            }

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

    private Map<Long, AttendanceGroup> loadGroupMap(Set<Long> groupIds) {
        if (groupIds.isEmpty()) return Collections.emptyMap();
        return attendanceGroupMapper.selectBatchIds(groupIds).stream()
                .collect(Collectors.toMap(AttendanceGroup::getId, g -> g));
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
                // 上下班都缺卡 → 视为旷工
                Integer startSt = record.getStartStatus();
                Integer endSt = record.getEndStatus();
                if (isMissing(startSt) && isMissing(endSt)) {
                    startSt = 3; // 旷工半天
                    endSt = 3;
                }
                item.setStartStatus(startSt);
                item.setStartStatusDesc(getStartStatusDesc(startSt));
                item.setEndStatus(endSt);
                item.setEndStatusDesc(getEndStatusDesc(endSt));
                if (record.getActualStartTime() != null) {
                    item.setClockIn(record.getActualStartTime().toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
                }
                if (record.getActualEndTime() != null) {
                    item.setClockOut(record.getActualEndTime().toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
                }

                if (isNormal(startSt)) normalDays++;
                if (startSt != null && startSt == 2) lateDays++;
                if (endSt != null && endSt == 2) earlyLeaveDays++;
                if (isAbsent(startSt)) absentDays++;
                if (isAbsent(endSt)) absentDays++;
                if (isMissing(startSt)) cardMissingDays++;
                if (isMissing(endSt)) cardMissingDays++;
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

    // ==================== 补卡申请 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupplementCardVO submitSupplementCard(SupplementCardSubmitDTO dto) {
        Long employeeId = dataScopeContext.getCurrentEmployeeId();
        if (employeeId == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        LocalDate attendanceDate = dto.getAttendanceDate();
        int cardType = dto.getCardType();
        if (cardType != 1 && cardType != 2) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的补卡类型");
        }

        // ① 缺卡校验：当天必须有对应的缺卡记录
        AttendanceRecord record = attendanceRecordMapper.selectOne(
                Wrappers.<AttendanceRecord>lambdaQuery()
                        .eq(AttendanceRecord::getEmployeeId, employeeId)
                        .eq(AttendanceRecord::getAttendanceDate, attendanceDate));
        if (record == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该日期无打卡记录");
        }
        boolean isMissing = (cardType == 1 && (record.getStartStatus() == null || record.getStartStatus() == 4))
                         || (cardType == 2 && (record.getEndStatus() == null || record.getEndStatus() == 4));
        if (!isMissing) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该日期无对应缺卡记录，无需补卡");
        }

        // ② 重复校验：同日期+同卡类型是否已提交
        Long dupCount = supplementCardRequestMapper.selectCount(
                Wrappers.<SupplementCardRequest>lambdaQuery()
                        .eq(SupplementCardRequest::getEmployeeId, employeeId)
                        .eq(SupplementCardRequest::getAttendanceDate, attendanceDate)
                        .eq(SupplementCardRequest::getCardType, cardType)
                        .in(SupplementCardRequest::getStatus, 2, 3));
        if (dupCount != null && dupCount > 0) {
            throw new BusinessException(ErrorCode.SUPPLEMENT_CARD_DUPLICATE);
        }

        // ③ 当月次数限制（≤2次，统计审批中+已通过）
        LocalDate firstOfMonth = attendanceDate.withDayOfMonth(1);
        LocalDate firstOfNextMonth = firstOfMonth.plusMonths(1);
        Long monthlyCount = supplementCardRequestMapper.selectCount(
                Wrappers.<SupplementCardRequest>lambdaQuery()
                        .eq(SupplementCardRequest::getEmployeeId, employeeId)
                        .in(SupplementCardRequest::getStatus, 2, 3)
                        .between(SupplementCardRequest::getCreateTime,
                                firstOfMonth.atStartOfDay(), firstOfNextMonth.atStartOfDay()));
        if (monthlyCount != null && monthlyCount >= 2) {
            throw new BusinessException(ErrorCode.SUPPLEMENT_CARD_LIMIT_EXCEEDED);
        }

        // ④ 保存
        SupplementCardRequest entity = new SupplementCardRequest();
        entity.setEmployeeId(employeeId);
        entity.setAttendanceDate(attendanceDate);
        entity.setCardType(cardType);
        entity.setReason(dto.getReason());
        entity.setStatus(2); // 审批中
        boolean saved = supplementCardRequestMapper.insert(entity) > 0;
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "提交补卡申请失败");
        }

        // ⑤ 创建审批实例
        ApprovalInstance instance = approvalFlowService.createInstance(
                ApprovalBizType.CARD_REPLENISH, entity.getId(), employeeId);
        entity.setApprovalInstanceId(instance.getId());
        supplementCardRequestMapper.updateById(entity);

        log.info("员工 {} 提交了补卡申请 (id={}), 日期={}, 类型={}, 审批实例={}",
                employeeId, entity.getId(), attendanceDate, cardType == 1 ? "上班卡" : "下班卡", instance.getId());

        SupplementCardVO vo = new SupplementCardVO();
        vo.setId(entity.getId());
        vo.setEmployeeId(employeeId);
        vo.setAttendanceDate(attendanceDate);
        vo.setCardType(cardType);
        vo.setCardTypeDesc(cardType == 1 ? "上班卡" : "下班卡");
        vo.setReason(dto.getReason());
        vo.setStatus(2);
        vo.setStatusDesc("审批中");
        vo.setMonthlyCount((int) (monthlyCount == null ? 1 : monthlyCount + 1));
        vo.setMonthlyLimit(2);
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    // ==================== 补卡列表 ====================

    @Override
    public Page<SupplementCardListVO> querySupplementCards(Long employeeId, Integer status,
                                                            LocalDate startDate, LocalDate endDate,
                                                            int page, int size) {
        QueryWrapper<SupplementCardRequest> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time");

        // 强制只看指定员工（个人中心用）
        if (employeeId != null) {
            wrapper.eq("employee_id", employeeId);
        } else {
            // 数据权限
            DataScopeEnum scope = dataScopeContext.getAttendanceScope();
        switch (scope) {
            case DEPT: {
                Set<Long> deptIds = dataScopeContext.getManagedDepartmentIds();
                if (deptIds == null || deptIds.isEmpty()) {
                    wrapper.eq("employee_id", dataScopeContext.getCurrentEmployeeId());
                } else {
                    wrapper.inSql("employee_id",
                            "SELECT e.id FROM employee e " +
                            "INNER JOIN employee_work_info ewi ON e.id = ewi.employee_id " +
                            "WHERE ewi.department_id IN (" + joinIds(deptIds) + ")");
                }
                break;
            }
            case SELF:
                wrapper.eq("employee_id", dataScopeContext.getCurrentEmployeeId());
                break;
            // ALL: no filter
        }
        } // end else (data scope)

        if (status != null) {
            wrapper.eq("status", status);
        }
        if (startDate != null) {
            wrapper.ge("attendance_date", startDate);
        }
        if (endDate != null) {
            wrapper.le("attendance_date", endDate);
        }

        Page<SupplementCardRequest> resultPage = supplementCardRequestMapper
                .selectPage(new Page<>(page, size), wrapper);

        // 批量查名称
        Set<Long> empIds = resultPage.getRecords().stream()
                .map(SupplementCardRequest::getEmployeeId).collect(Collectors.toSet());
        Map<Long, String> empNameMap = loadEmpNameMap(empIds);
        Map<Long, String> deptNameMap = loadDeptNameMap(empIds);

        List<SupplementCardListVO> voList = resultPage.getRecords().stream().map(r -> {
            SupplementCardListVO vo = new SupplementCardListVO();
            vo.setId(r.getId());
            vo.setEmployeeId(r.getEmployeeId());
            vo.setEmployeeName(empNameMap.getOrDefault(r.getEmployeeId(), ""));
            vo.setDepartmentName(deptNameMap.getOrDefault(r.getEmployeeId(), ""));
            vo.setAttendanceDate(r.getAttendanceDate());
            vo.setCardType(r.getCardType());
            vo.setCardTypeDesc(r.getCardType() == 1 ? "上班卡" : "下班卡");
            vo.setReason(r.getReason());
            vo.setStatus(r.getStatus());
            vo.setStatusDesc(getSupplementStatusDesc(r.getStatus()));
            vo.setCreateTime(r.getCreateTime());
            return vo;
        }).collect(Collectors.toList());

        Page<SupplementCardListVO> result = new Page<>(page, size, resultPage.getTotal());
        result.setRecords(voList);
        return result;
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
     * 下班卡状态判定（固定班/排班制）
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

    /**
     * 弹性班上��卡判定：在 flexStartTime ~ flexEndTime 内打卡为正常
     */
    private int judgeFlexStartStatus(LocalDateTime actualTime, LocalTime flexStartTime, LocalTime flexEndTime,
                                      Integer lateThreshold) {
        if (flexStartTime == null || flexEndTime == null) return 1; // 未配置弹性时间则默认正常
        if (lateThreshold == null) lateThreshold = 15;
        LocalTime actual = actualTime.toLocalTime();
        LocalTime lateLine = flexEndTime.plusMinutes(lateThreshold);

        if (actual.isBefore(flexStartTime)) {
            return 1; // 提前打卡视为正常
        } else if (!actual.isAfter(flexEndTime)) {
            return 1; // 弹性窗口内正常
        } else if (!actual.isAfter(lateLine)) {
            return 2; // 超过弹性窗口但在阈值内 → 迟到
        } else {
            return 3; // 旷工半天
        }
    }

    /**
     * 弹性班下班判定：按实际工作时长判定
     */
    private int judgeFlexEndStatus(LocalDateTime startTime, LocalDateTime endTime,
                                    java.math.BigDecimal workHours, Integer earlyLeaveThreshold) {
        if (workHours == null) workHours = new java.math.BigDecimal("8");
        if (earlyLeaveThreshold == null) earlyLeaveThreshold = 15;
        // 扣午休 1 小时
        long actualMinutes = java.time.Duration.between(startTime, endTime).toMinutes() - 60;
        long requiredMinutes = workHours.multiply(new java.math.BigDecimal("60")).longValue();

        if (actualMinutes >= requiredMinutes) {
            return 1; // 正常
        } else if (actualMinutes >= requiredMinutes - earlyLeaveThreshold) {
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

    private String getSupplementStatusDesc(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 1: return "草稿";
            case 2: return "审批中";
            case 3: return "已通过";
            case 4: return "已拒绝";
            default: return "未知";
        }
    }

    private String joinIds(Set<Long> ids) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Long id : ids) {
            if (i > 0) sb.append(",");
            sb.append(id);
            i++;
        }
        return sb.toString();
    }

    // ==================== 补卡审批回调 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onApproved(ApprovalBizType bizType, Long bizId) {
        if (bizType != ApprovalBizType.CARD_REPLENISH) return;

        SupplementCardRequest card = supplementCardRequestMapper.selectById(bizId);
        if (card == null) return;
        card.setStatus(3); // 已通过
        supplementCardRequestMapper.updateById(card);

        // 补卡：取考勤组规定的上下班时间作为实际打卡时间
        AttendanceRecord record = attendanceRecordMapper.selectOne(
                Wrappers.<AttendanceRecord>lambdaQuery()
                        .eq(AttendanceRecord::getEmployeeId, card.getEmployeeId())
                        .eq(AttendanceRecord::getAttendanceDate, card.getAttendanceDate()));
        if (record != null) {
            AttendanceGroup group = matchAttendanceGroup(card.getEmployeeId());
            LocalTime scheduledTime;
            if (card.getCardType() == 1) {
                // 上班卡：取规定上班时间
                scheduledTime = group != null && group.getStartTime() != null
                        ? group.getStartTime() : LocalTime.of(9, 0);
                record.setActualStartTime(LocalDateTime.of(card.getAttendanceDate(), scheduledTime));
                int status = judgeStartStatus(record.getActualStartTime(),
                        scheduledTime, group != null ? group.getLateThreshold() : 15);
                record.setStartStatus(status);
            } else {
                // 下班卡：取规定下班时间
                scheduledTime = group != null && group.getEndTime() != null
                        ? group.getEndTime() : LocalTime.of(18, 0);
                record.setActualEndTime(LocalDateTime.of(card.getAttendanceDate(), scheduledTime));
                int status = judgeEndStatus(record.getActualEndTime(),
                        scheduledTime, group != null ? group.getEarlyLeaveThreshold() : 15);
                record.setEndStatus(status);
            }
            attendanceRecordMapper.updateById(record);
        }
        log.info("补卡审批通过: cardId={}, employeeId={}, date={}, cardType={}",
                bizId, card.getEmployeeId(), card.getAttendanceDate(), card.getCardType());
    }

    @Override
    public void onRejected(ApprovalBizType bizType, Long bizId) {
        if (bizType != ApprovalBizType.CARD_REPLENISH) return;

        SupplementCardRequest card = supplementCardRequestMapper.selectById(bizId);
        if (card == null) return;
        card.setStatus(4); // 已拒绝
        supplementCardRequestMapper.updateById(card);
        log.info("补卡审批已拒绝: cardId={}", bizId);
    }
}
