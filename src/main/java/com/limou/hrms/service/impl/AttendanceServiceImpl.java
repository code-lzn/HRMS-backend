package com.limou.hrms.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.AttendanceConstant;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.ApprovalRecordMapper;
import com.limou.hrms.mapper.AttendanceMapper;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.ApprovalRecordStatusEnum;
import com.limou.hrms.model.enums.AttendanceStatusEnum;
import com.limou.hrms.model.enums.PunchTypeEnum;
import com.limou.hrms.model.vo.AttendanceCalendarVO;
import com.limou.hrms.model.vo.AttendanceVO;
import com.limou.hrms.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 考勤打卡服务实现
 */
@Service
@Slf4j
public class AttendanceServiceImpl extends ServiceImpl<AttendanceMapper, Attendance>
        implements AttendanceService {

    @Resource
    private EmployeeService employeeService;

    @Resource
    private AttendanceGroupService attendanceGroupService;

    @Resource
    @Lazy
    private LeaveService leaveService;

    @Resource
    private HolidayConfigService holidayConfigService;

    @Resource
    @Lazy
    private ApprovalService approvalService;

    @Resource
    private ApprovalRecordMapper approvalRecordMapper;

    @Resource
    private com.limou.hrms.mapper.DepartmentMapper departmentMapper;

    @Resource
    private PlatformTransactionManager transactionManager;

    /**
     * 上下班打卡
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttendanceVO punch(Long userId, Integer punchType, String location) {
        Employee emp = employeeService.getByUserId(userId);
        Date now = new Date();
        String today = DateUtil.formatDate(now);

        // 休息日打卡：无论何时打卡都记为休息
        boolean isWorkDay = holidayConfigService.isWorkDay(now);

        // 查今天的打卡记录
        Attendance record = this.lambdaQuery()
                .eq(Attendance::getEmployeeId, emp.getId())
                .eq(Attendance::getAttendanceDate, DateUtil.parseDate(today))
                .one();
        //上班打卡
        boolean isPunchIn = (punchType == null || punchType == 0);

        if (record == null) {
            // 首次打卡，新建记录
            record = new Attendance();
            record.setEmployeeId(emp.getId());
            record.setUserId(userId);
            record.setAttendanceDate(DateUtil.parseDate(today));
            record.setStatus(isWorkDay ? AttendanceStatusEnum.NORMAL.getValue()
                                       : AttendanceStatusEnum.REST.getValue());
            record.setPunchInType(PunchTypeEnum.WEB.getValue());
            record.setPunchOutType(PunchTypeEnum.WEB.getValue());
        }

        // 休息日：直接记录打卡时间，状态保持 REST，不判断迟到/早退
        if (!isWorkDay) {
            if (isPunchIn) {
                record.setPunchInTime(now);
                record.setPunchInLocation(location);
            } else {
                record.setPunchOutTime(now);
                record.setPunchOutLocation(location);
            }
            record.setStatus(AttendanceStatusEnum.REST.getValue());
            boolean saved = this.saveOrUpdate(record);
            ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "打卡失败");
            return convertToVO(record, emp);
        }

        // 工作日：正常判断迟到/早退
        AttendanceGroup group = attendanceGroupService.getGroupByEmployeeId(emp.getId());

        if (isPunchIn) {
            ThrowUtils.throwIf(record.getPunchInTime() != null,
                    ErrorCode.ATTENDANCE_DUPLICATE_ERROR);
            int hour = DateUtil.hour(now, true);
            int minute = DateUtil.minute(now);
            ThrowUtils.throwIf(hour < 8,
                    ErrorCode.ATTENDANCE_TIME_ERROR, "上班打卡时间不得早于8:00");
            record.setPunchInTime(now);
            record.setPunchInLocation(location);

            // 上午请假：跳过迟到判断
            boolean isAmLeave = record.getHalfDayLeave() != null && record.getHalfDayLeave() == 1;
            if (!isAmLeave) {
                int workStartHour = AttendanceConstant.DEFAULT_WORK_START_HOUR;
                int workStartMinute = 0;
                int lateThreshold = AttendanceConstant.LATE_GRACE_MINUTES;
                if (group != null && group.getWorkStartTime() != null) {
                    workStartHour = DateUtil.hour(group.getWorkStartTime(), true);
                    workStartMinute = DateUtil.minute(group.getWorkStartTime());
                    lateThreshold = group.getLateThreshold() != null ? group.getLateThreshold() : lateThreshold;
                }

                int workStartTotal = workStartHour * 60 + workStartMinute;
                int lateMinutes = hour * 60 + minute - workStartTotal;
                if (lateMinutes > 0) {
                    record.setLateMinutes(lateMinutes);
                    record.setStatus(lateMinutes > lateThreshold
                            ? AttendanceStatusEnum.SEVERE_LATE.getValue()
                            : AttendanceStatusEnum.LATE.getValue());
                }
            }
        } else {
            ThrowUtils.throwIf(record.getPunchOutTime() != null,
                    ErrorCode.ATTENDANCE_DUPLICATE_ERROR);
            record.setPunchOutTime(now);
            record.setPunchOutLocation(location);

            // 下午请假：跳过早退判断
            boolean isPmLeave = record.getHalfDayLeave() != null && record.getHalfDayLeave() == 2;
            if (!isPmLeave) {
                int workEndHour = AttendanceConstant.DEFAULT_WORK_END_HOUR;
                int workEndMinute = 0;
                int earlyThreshold = AttendanceConstant.LATE_GRACE_MINUTES;
                if (group != null && group.getWorkEndTime() != null) {
                    workEndHour = DateUtil.hour(group.getWorkEndTime(), true);
                    workEndMinute = DateUtil.minute(group.getWorkEndTime());
                    earlyThreshold = group.getEarlyThreshold() != null ? group.getEarlyThreshold() : earlyThreshold;
                }

                int hour = DateUtil.hour(now, true);
                int minute = DateUtil.minute(now);
                int workEndTotal = workEndHour * 60 + workEndMinute;
                int earlyMinutes = workEndTotal - (hour * 60 + minute);

                if (earlyMinutes > earlyThreshold) {
                    record.setEarlyMinutes(earlyMinutes);
                    if (Objects.equals(record.getStatus(), AttendanceStatusEnum.LATE.getValue())
                            || Objects.equals(record.getStatus(), AttendanceStatusEnum.SEVERE_LATE.getValue())) {
                        record.setStatus(AttendanceStatusEnum.LATE_AND_EARLY.getValue());
                    } else {
                        record.setStatus(AttendanceStatusEnum.LEAVE_EARLY.getValue());
                    }
                } else {
                    if (!Objects.equals(record.getStatus(), AttendanceStatusEnum.LATE.getValue())
                            && !Objects.equals(record.getStatus(), AttendanceStatusEnum.LEAVE_EARLY.getValue())
                            && !Objects.equals(record.getStatus(), AttendanceStatusEnum.LATE_AND_EARLY.getValue())) {
                        record.setStatus(AttendanceStatusEnum.NORMAL.getValue());
                    }
                }
            }
        }

        // 上下班打卡间隔不足2小时，直接算旷工
        if (record.getPunchInTime() != null && record.getPunchOutTime() != null) {
            long intervalMs = record.getPunchOutTime().getTime() - record.getPunchInTime().getTime();
            if (intervalMs < 2 * 60 * 60 * 1000) {
                record.setStatus(AttendanceStatusEnum.ABSENT.getValue());
                record.setLateMinutes(null);
                record.setEarlyMinutes(null);
            }
        }

        boolean saved = this.saveOrUpdate(record);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "打卡失败");

        return convertToVO(record, emp);
    }

    @Override
    public AttendanceCalendarVO getCalendar(Long userId, String month) {
        Employee emp = employeeService.getByUserId(userId);
        // month 格式: yyyy-MM
        Date monthStart = DateUtil.parseDate(month + "-01");
        Date monthEnd = DateUtil.endOfMonth(monthStart);

        List<Attendance> records = this.lambdaQuery()
                .eq(Attendance::getEmployeeId, emp.getId())
                .ge(Attendance::getAttendanceDate, DateUtil.formatDate(monthStart))
                .le(Attendance::getAttendanceDate, DateUtil.formatDate(monthEnd))
                .ge(emp.getHireDate() != null, Attendance::getAttendanceDate,
                        DateUtil.formatDate(emp.getHireDate()))
                .orderByAsc(Attendance::getAttendanceDate)
                .list();

        AttendanceCalendarVO vo = new AttendanceCalendarVO();
        vo.setMonth(month);
        vo.setNormalDays(0);
        vo.setLateDays(0);
        vo.setLeaveDays(0);
        vo.setMissingDays(0);
        vo.setAbsentDays(0);

        Map<String, Integer> dailyStatus = new LinkedHashMap<>();
        Map<String, String> dailyStatusText = new LinkedHashMap<>();
        List<String> makeupDates = new ArrayList<>();

        for (Attendance r : records) {
            String dateStr = DateUtil.formatDate(r.getAttendanceDate());
            dailyStatus.put(dateStr, r.getStatus());
            dailyStatusText.put(dateStr, resolveStatusText(r));

            AttendanceStatusEnum statusEnum = AttendanceStatusEnum.getEnumByValue(r.getStatus());
            if (statusEnum != null) {
                switch (statusEnum) {
                    case NORMAL: vo.setNormalDays(vo.getNormalDays() + 1); break;
                    case LATE:
                    case LEAVE_EARLY:
                    case LATE_AND_EARLY:
                    case SEVERE_LATE:
                        vo.setLateDays(vo.getLateDays() + 1); break;
                    case LEAVE: vo.setLeaveDays(vo.getLeaveDays() + 1); break;
                    case ABSENT: vo.setAbsentDays(vo.getAbsentDays() + 1); break;
                    case MISSING:
                        vo.setMissingDays(vo.getMissingDays() + 1);
                        makeupDates.add(dateStr);
                        break;
                    case MISS_IN:
                    case MISS_OUT:
                        makeupDates.add(dateStr);
                        break;
                    default: break;
                }
            }
        }

        vo.setDailyStatus(dailyStatus);
        vo.setDailyStatusText(dailyStatusText);
        vo.setMakeupAvailableDates(makeupDates);
        return vo;
    }

    @Override
    public List<AttendanceVO> getMonthRecords(Long userId, String month) {
        Employee emp = employeeService.getByUserId(userId);
        Date monthStart = DateUtil.parseDate(month + "-01");
        Date monthEnd = DateUtil.endOfMonth(monthStart);

        List<Attendance> records = this.lambdaQuery()
                .eq(Attendance::getEmployeeId, emp.getId())
                .ge(Attendance::getAttendanceDate, DateUtil.formatDate(monthStart))
                .le(Attendance::getAttendanceDate, DateUtil.formatDate(monthEnd))
                .orderByDesc(Attendance::getAttendanceDate)
                .list();

        return records.stream().map(r -> convertToVO(r, emp)).collect(Collectors.toList());
    }

    @Override
    public AttendanceVO getTodayStatus(Long userId) {
        Employee emp = employeeService.getByUserId(userId);
        String today = DateUtil.formatDate(new Date());

        Attendance record = this.lambdaQuery()
                .eq(Attendance::getEmployeeId, emp.getId())
                .eq(Attendance::getAttendanceDate, DateUtil.parseDate(today))
                .one();

        if (record == null) {
            AttendanceVO vo = new AttendanceVO();
            vo.setAttendanceDate(DateUtil.parseDate(today));
            vo.setStatus(AttendanceStatusEnum.MISSING.getValue());
            vo.setStatusText("未打卡");
            vo.setEmployeeName(emp.getEmployeeName());
            if (emp.getDepartmentId() != null) {
                Department dept = departmentMapper.selectById(emp.getDepartmentId());
                vo.setDeptName(dept != null ? dept.getDeptName() : null);
            }
            return vo;
        }
        return convertToVO(record, emp);
    }

    // ========== 日终处理 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateDailyRecords(String date) {
        Date targetDate = DateUtil.parseDate(date);
        boolean isWorkDay = holidayConfigService.isWorkDay(targetDate);

        List<Employee> employees = employeeService.lambdaQuery()
                .eq(Employee::getIsDeleted, 0)
                .le(Employee::getHireDate, date)
                .list();
        if (employees.isEmpty()) return 0;

        // 查询当天已有的考勤记录
        List<Attendance> existingRecords = this.lambdaQuery()
                .eq(Attendance::getAttendanceDate, date)
                .list();
        Set<Long> existingEmpIds = existingRecords.stream()
                .map(Attendance::getEmployeeId).collect(Collectors.toSet());

        // 工作日：跳过请假员工；非工作日：全员休息
        Set<Long> leaveEmpIds = Collections.emptySet();
        if (isWorkDay) {
            List<Leave> todayLeaves = leaveService.lambdaQuery()
                    .eq(Leave::getStatus, 1)
                    .le(Leave::getStartDate, date)
                    .ge(Leave::getEndDate, date)
                    .list();
            leaveEmpIds = todayLeaves.stream()
                    .map(Leave::getEmployeeId).collect(Collectors.toSet());
        }

        int created = 0;
        Date now = new Date();

        // 非工作日：修正已有记录中无打卡的非休息状态 → 休息
        if (!isWorkDay) {
            for (Attendance r : existingRecords) {
                if (r.getPunchInTime() == null && r.getPunchOutTime() == null
                        && !Objects.equals(r.getStatus(), AttendanceStatusEnum.REST.getValue())) {
                    r.setStatus(AttendanceStatusEnum.REST.getValue());
                    r.setUpdateTime(now);
                    this.updateById(r);
                    created++;
                }
            }
        }

        for (Employee emp : employees) {
            if (existingEmpIds.contains(emp.getId())) continue;
            if (leaveEmpIds.contains(emp.getId())) continue;

            Attendance record = new Attendance();
            record.setEmployeeId(emp.getId());
            record.setUserId(emp.getUserId());
            record.setAttendanceDate(targetDate);

            if (isWorkDay) {
                // 工作日统一初始为正常，19:00 终评再根据实际打卡情况判定
                record.setStatus(AttendanceStatusEnum.NORMAL.getValue());
            } else {
                record.setStatus(AttendanceStatusEnum.REST.getValue());
            }

            record.setCreateTime(now);
            record.setUpdateTime(now);
            this.save(record);
            created++;
        }

        return created;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int evaluateEndOfDay(String date) {
        Date targetDate = DateUtil.parseDate(date);

        List<Attendance> records = this.lambdaQuery()
                .eq(Attendance::getAttendanceDate, date)
                .list();

        int updated = 0;
        for (Attendance r : records) {
            // 休息日不评估
            if (Objects.equals(r.getStatus(), AttendanceStatusEnum.REST.getValue())) {
                continue;
            }

            boolean hasIn = r.getPunchInTime() != null;
            boolean hasOut = r.getPunchOutTime() != null;

            // 半天请假：不按缺卡处理对应的半段
            Integer halfDay = r.getHalfDayLeave();
            boolean isAmLeave = halfDay != null && halfDay == 1;
            boolean isPmLeave = halfDay != null && halfDay == 2;

            if (!hasIn && !hasOut) {
                // 全天未打卡 → 缺卡
                if (!Objects.equals(r.getStatus(), AttendanceStatusEnum.MISSING.getValue())) {
                    r.setStatus(AttendanceStatusEnum.MISSING.getValue());
                    r.setUpdateTime(new Date());
                    this.updateById(r);
                    updated++;
                }
            } else if (hasIn && !hasOut) {
                // 下午请假：下班缺卡属正常
                if (isPmLeave) {
                    r.setStatus(AttendanceStatusEnum.NORMAL.getValue());
                    r.setUpdateTime(new Date());
                    this.updateById(r);
                } else {
                    r.setStatus(AttendanceStatusEnum.MISS_OUT.getValue());
                    r.setUpdateTime(new Date());
                    this.updateById(r);
                    updated++;
                    createAnomalyApproval(r);
                }
            } else if (!hasIn && hasOut) {
                // 上午请假：上班缺卡属正常
                if (isAmLeave) {
                    r.setStatus(AttendanceStatusEnum.NORMAL.getValue());
                    r.setUpdateTime(new Date());
                    this.updateById(r);
                } else {
                    r.setStatus(AttendanceStatusEnum.MISS_IN.getValue());
                    r.setUpdateTime(new Date());
                    this.updateById(r);
                    updated++;
                    createAnomalyApproval(r);
                }
            }
        }

        return updated;
    }

    @Override
    public int syncAnomalyApprovals() {
        // 查询所有已拒绝的考勤异常审批
        List<ApprovalRecord> rejectedRecords = approvalRecordMapper.selectList(
                new LambdaQueryWrapper<ApprovalRecord>()
                        .eq(ApprovalRecord::getBusinessType, "ATTENDANCE_ANOMALY")
                        .eq(ApprovalRecord::getStatus, ApprovalRecordStatusEnum.REJECTED.getValue())
        );

        int synced = 0;
        for (ApprovalRecord record : rejectedRecords) {
            Attendance attendance = this.getById(record.getBusinessId());
            if (attendance == null) continue;

            // 仅当考勤状态仍为缺卡异常时才恢复
            Integer status = attendance.getStatus();
            if (Objects.equals(status, AttendanceStatusEnum.MISS_OUT.getValue())
                    || Objects.equals(status, AttendanceStatusEnum.MISS_IN.getValue())) {
                attendance.setStatus(AttendanceStatusEnum.NORMAL.getValue());
                attendance.setUpdateTime(new Date());
                this.updateById(attendance);
                synced++;
            }
        }

        return synced;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int correctTodayLateStatus() {
        String today = DateUtil.formatDate(new Date());
        Date now = new Date();
        int nowHour = DateUtil.hour(now, true);
        int nowMinute = DateUtil.minute(now);
        int nowTotalMinutes = nowHour * 60 + nowMinute;

        List<Attendance> records = this.lambdaQuery()
                .eq(Attendance::getAttendanceDate, today)
                .in(Attendance::getStatus, Arrays.asList(
                        AttendanceStatusEnum.LATE.getValue(),
                        AttendanceStatusEnum.SEVERE_LATE.getValue()))
                .isNull(Attendance::getPunchInTime)
                .isNull(Attendance::getPunchOutTime)
                .list();

        int corrected = 0;
        for (Attendance r : records) {
            AttendanceGroup group = attendanceGroupService.getGroupByEmployeeId(r.getEmployeeId());
            int workStartHour = AttendanceConstant.DEFAULT_WORK_START_HOUR;
            int workStartMinute = 0;
            int lateGrace = AttendanceConstant.LATE_GRACE_MINUTES;
            if (group != null && group.getWorkStartTime() != null) {
                workStartHour = DateUtil.hour(group.getWorkStartTime(), true);
                workStartMinute = DateUtil.minute(group.getWorkStartTime());
                lateGrace = group.getLateThreshold() != null ? group.getLateThreshold() : lateGrace;
            }
            int workStartTotal = workStartHour * 60 + workStartMinute;
            int thresholdMinutes = workStartTotal + lateGrace;
            if (nowTotalMinutes <= thresholdMinutes) {
                r.setStatus(AttendanceStatusEnum.NORMAL.getValue());
                r.setLateMinutes(null);
                r.setUpdateTime(now);
                this.updateById(r);
                corrected++;
            }
        }

        return corrected;
    }

    /**
     * 为缺卡异常创建审批记录，上报部门负责人
     */
    private void createAnomalyApproval(Attendance record) {
        try {
            DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            TransactionStatus status = transactionManager.getTransaction(def);
            try {
                Employee emp = employeeService.getById(record.getEmployeeId());
                if (emp == null) {
                    transactionManager.commit(status);
                    return;
                }

                Map<Integer, Long> overrides = new HashMap<>();
                overrides.put(2, emp.getId());

                approvalService.startApproval(
                        "ATTENDANCE_ANOMALY",
                        record.getId(),
                        emp.getId(),
                        emp.getEmployeeName(),
                        emp.getDepartmentId(),
                        overrides
                );
                transactionManager.commit(status);
            } catch (Exception ex) {
                transactionManager.rollback(status);
                throw ex;
            }
        } catch (Exception e) {
            log.warn("创建考勤异常审批失败，员工ID={}, 日期={}, 原因: {}",
                    record.getEmployeeId(), record.getAttendanceDate(), e.getMessage());
        }
    }

    // ========== 私有方法 ==========

    private String resolveStatusText(Attendance record) {
        if (record.getLateMinutes() != null && record.getLateMinutes() > 0
                && record.getEarlyMinutes() != null && record.getEarlyMinutes() > 0) {
            return AttendanceStatusEnum.LATE_AND_EARLY.getText();
        }
        AttendanceStatusEnum status = AttendanceStatusEnum.getEnumByValue(record.getStatus());
        return status != null ? status.getText() : "未知";
    }

    private AttendanceVO convertToVO(Attendance record, Employee emp) {
        AttendanceVO vo = new AttendanceVO();
        BeanUtils.copyProperties(record, vo);
        vo.setStatusText(resolveStatusText(record));
        vo.setEmployeeName(emp != null ? emp.getEmployeeName() : null);
        if (emp != null && emp.getDepartmentId() != null) {
            Department dept = departmentMapper.selectById(emp.getDepartmentId());
            vo.setDeptName(dept != null ? dept.getDeptName() : null);
        }
        return vo;
    }
}
