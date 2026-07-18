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
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * 上下班打卡
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttendanceVO punch(Long userId, Integer punchType, String location) {
        Employee emp = employeeService.getByUserId(userId);
        Date now = new Date();
        String today = DateUtil.formatDate(now);

        // 查今天的打卡记录
        Attendance record = this.lambdaQuery()
                .eq(Attendance::getEmployeeId, emp.getId())
                .eq(Attendance::getAttendanceDate, DateUtil.parseDate(today))
                .one();

        boolean isPunchIn = (punchType == null || punchType == 0);

        if (record == null) {
            // 首次打卡，新建记录
            record = new Attendance();
            record.setEmployeeId(emp.getId());
            record.setUserId(userId);
            record.setAttendanceDate(DateUtil.parseDate(today));
            record.setStatus(AttendanceStatusEnum.NORMAL.getValue());
            record.setPunchInType(PunchTypeEnum.WEB.getValue());
            record.setPunchOutType(PunchTypeEnum.WEB.getValue());
        }

        if (isPunchIn) {
            ThrowUtils.throwIf(record.getPunchInTime() != null,
                    ErrorCode.ATTENDANCE_DUPLICATE_ERROR);
            record.setPunchInTime(now);
            record.setPunchInLocation(location);

            // 获取员工考勤组，使用组内规则判断迟到
            AttendanceGroup group = attendanceGroupService.getGroupByEmployeeId(emp.getId());
            int workStartHour = AttendanceConstant.DEFAULT_WORK_START_HOUR;
            int lateGrace = AttendanceConstant.LATE_GRACE_MINUTES;
            if (group != null && group.getWorkStartTime() != null) {
                workStartHour = DateUtil.hour(group.getWorkStartTime(), true);
                lateGrace = group.getLateThreshold() != null ? group.getLateThreshold() : lateGrace;
            }

            int hour = DateUtil.hour(now, true);
            int minute = DateUtil.minute(now);
            int thresholdMinutes = workStartHour * 60 + lateGrace;
            if (hour * 60 + minute > thresholdMinutes) {
                record.setStatus(AttendanceStatusEnum.LATE.getValue());
            }
        } else {
            ThrowUtils.throwIf(record.getPunchOutTime() != null,
                    ErrorCode.ATTENDANCE_DUPLICATE_ERROR);
            record.setPunchOutTime(now);
            record.setPunchOutLocation(location);

            // 获取考勤组规则判断早退
            AttendanceGroup group = attendanceGroupService.getGroupByEmployeeId(emp.getId());
            int workEndHour = AttendanceConstant.DEFAULT_WORK_END_HOUR;
            int earlyThreshold = AttendanceConstant.LATE_GRACE_MINUTES;
            if (group != null && group.getWorkEndTime() != null) {
                workEndHour = DateUtil.hour(group.getWorkEndTime(), true);
                earlyThreshold = group.getEarlyThreshold() != null ? group.getEarlyThreshold() : earlyThreshold;
            }

            int hour = DateUtil.hour(now, true);
            int minute = DateUtil.minute(now);
            int earlyMinutes = workEndHour * 60 - (hour * 60 + minute);

            if (earlyMinutes > earlyThreshold) {
                // 早退：下班时间比规定时间早，且超过阈值
                record.setEarlyMinutes(earlyMinutes);
                // 如果已经迟到，保持迟到状态（叠加违规取首次违规）
                if (!Objects.equals(record.getStatus(), AttendanceStatusEnum.LATE.getValue())) {
                    record.setStatus(AttendanceStatusEnum.LEAVE_EARLY.getValue());
                }
            } else {
                // 正常下班：保持原有状态（迟到仍然是迟到，正常则保持正常）
                if (!Objects.equals(record.getStatus(), AttendanceStatusEnum.LATE.getValue())
                        && !Objects.equals(record.getStatus(), AttendanceStatusEnum.LEAVE_EARLY.getValue())) {
                    record.setStatus(AttendanceStatusEnum.NORMAL.getValue());
                }
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

        Map<String, Integer> dailyStatus = new LinkedHashMap<>();
        List<String> makeupDates = new ArrayList<>();

        for (Attendance r : records) {
            String dateStr = DateUtil.formatDate(r.getAttendanceDate());
            dailyStatus.put(dateStr, r.getStatus());

            switch (r.getStatus()) {
                case 0: vo.setNormalDays(vo.getNormalDays() + 1); break;
                case 1: vo.setLateDays(vo.getLateDays() + 1); break;
                case 4: vo.setLeaveDays(vo.getLeaveDays() + 1); break;
                // 缺卡---可以进行补卡
                case 3:
                    vo.setMissingDays(vo.getMissingDays() + 1);
                    makeupDates.add(dateStr);
                    break;
                default: break;
            }
        }

        vo.setDailyStatus(dailyStatus);
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

        int defaultStatus = isWorkDay ? AttendanceStatusEnum.LATE.getValue()
                                      : AttendanceStatusEnum.REST.getValue();

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
            record.setStatus(defaultStatus);
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

            if (!hasIn && !hasOut) {
                // 全天未打卡 → 旷工
                if (!Objects.equals(r.getStatus(), AttendanceStatusEnum.ABSENT.getValue())) {
                    r.setStatus(AttendanceStatusEnum.ABSENT.getValue());
                    r.setUpdateTime(new Date());
                    this.updateById(r);
                    updated++;
                }
            } else if (hasIn && !hasOut) {
                // 上班打卡了但下班没打卡 → 下班缺卡
                r.setStatus(AttendanceStatusEnum.MISS_OUT.getValue());
                r.setUpdateTime(new Date());
                this.updateById(r);
                updated++;
                createAnomalyApproval(r);
            } else if (!hasIn && hasOut) {
                // 下班打卡了但上班没打卡 → 上班缺卡
                r.setStatus(AttendanceStatusEnum.MISS_IN.getValue());
                r.setUpdateTime(new Date());
                this.updateById(r);
                updated++;
                createAnomalyApproval(r);
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

    /**
     * 为缺卡异常创建审批记录，上报部门负责人
     */
    private void createAnomalyApproval(Attendance record) {
        try {
            Employee emp = employeeService.getById(record.getEmployeeId());
            if (emp == null) return;

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
        } catch (Exception e) {
            log.warn("创建考勤异常审批失败，员工ID={}, 日期={}, 原因: {}",
                    record.getEmployeeId(), record.getAttendanceDate(), e.getMessage());
        }
    }

    // ========== 私有方法 ==========

    private AttendanceVO convertToVO(Attendance record, Employee emp) {
        AttendanceVO vo = new AttendanceVO();
        BeanUtils.copyProperties(record, vo);
        AttendanceStatusEnum status = AttendanceStatusEnum.getEnumByValue(record.getStatus());
        vo.setStatusText(status != null ? status.getText() : "未知");
        vo.setEmployeeName(emp != null ? emp.getEmployeeName() : null);
        if (emp != null && emp.getDepartmentId() != null) {
            Department dept = departmentMapper.selectById(emp.getDepartmentId());
            vo.setDeptName(dept != null ? dept.getDeptName() : null);
        }
        return vo;
    }
}
