package com.limou.hrms.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.AttendanceConstant;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.AttendanceMapper;
import com.limou.hrms.model.dto.attendance.HRAttendanceDTO;
import com.limou.hrms.model.dto.attendance.HRAttendanceQueryRequest;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.AttendanceStatusEnum;
import com.limou.hrms.model.enums.DataScopeEnum;
import com.limou.hrms.model.enums.LeaveTypeEnum;
import com.limou.hrms.model.vo.HRAttendanceVO;
import com.limou.hrms.model.vo.PageResult;
import com.limou.hrms.service.AttendanceGroupService;
import com.limou.hrms.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HRAttendanceServiceImpl extends ServiceImpl<AttendanceMapper, Attendance>
        implements HRAttendanceService {

    @Resource
    private EmployeeService employeeService;

    @Resource
    private DepartmentService departmentService;

    @Resource
    private LeaveService leaveService;

    @Resource
    private AttendanceService attendanceService;

    @Resource
    private PermissionService permissionService;

    @Resource
    private UserService userService;

    @Resource
    private AttendanceGroupService attendanceGroupService;

    @Override
    public PageResult<HRAttendanceVO> queryAttendance(HRAttendanceQueryRequest request, HttpServletRequest httpRequest) {
        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 10;

        LambdaQueryWrapper<Attendance> queryWrapper = new LambdaQueryWrapper<>();

        User currentUser = userService.getLoginUser(httpRequest);
        Integer dataScope = permissionService.getUserDataScope(currentUser.getId());
        applyDataScopeFilter(queryWrapper, currentUser.getId(), dataScope);

        if (request.getEmployeeName() != null && !request.getEmployeeName().isEmpty()) {
            List<Long> empIds = employeeService.lambdaQuery()
                    .like(Employee::getEmployeeName, request.getEmployeeName())
                    .list().stream().map(Employee::getId).collect(Collectors.toList());
            if (!empIds.isEmpty()) {
                queryWrapper.in(Attendance::getEmployeeId, empIds);
            } else {
                queryWrapper.eq(Attendance::getEmployeeId, 0L);
            }
        }

        if (request.getEmployeeNo() != null && !request.getEmployeeNo().isEmpty()) {
            List<Long> empIds = employeeService.lambdaQuery()
                    .eq(Employee::getEmployeeNo, request.getEmployeeNo())
                    .list().stream().map(Employee::getId).collect(Collectors.toList());
            if (!empIds.isEmpty()) {
                queryWrapper.in(Attendance::getEmployeeId, empIds);
            } else {
                queryWrapper.eq(Attendance::getEmployeeId, 0L);
            }
        }

        if (request.getDepartmentId() != null) {
            List<Long> empIds = employeeService.lambdaQuery()
                    .eq(Employee::getDepartmentId, request.getDepartmentId())
                    .list().stream().map(Employee::getId).collect(Collectors.toList());
            if (!empIds.isEmpty()) {
                queryWrapper.in(Attendance::getEmployeeId, empIds);
            } else {
                queryWrapper.eq(Attendance::getEmployeeId, 0L);
            }
        }

        if (request.getMonth() != null && !request.getMonth().isEmpty()) {
            String[] parts = request.getMonth().split("-");
            if (parts.length == 2) {
                String startDate = request.getMonth() + "-01";
                String endDate = DateUtil.endOfMonth(DateUtil.parseDate(startDate)).toString("yyyy-MM-dd");
                queryWrapper.ge(Attendance::getAttendanceDate, startDate)
                        .le(Attendance::getAttendanceDate, endDate);
            }
        }

        if (request.getStatus() != null) {
            queryWrapper.eq(Attendance::getStatus, request.getStatus());
        }

        if (request.getPunchType() != null) {
            if (request.getPunchType() == 1) {
                queryWrapper.isNotNull(Attendance::getPunchInTime);
            } else if (request.getPunchType() == 2) {
                queryWrapper.isNotNull(Attendance::getPunchOutTime);
            }
        }

        if (request.getSortField() != null && !request.getSortField().isEmpty()) {
            if ("attendanceDate".equals(request.getSortField())) {
                queryWrapper.orderBy(true, "asc".equalsIgnoreCase(request.getSortOrder()),
                        Attendance::getAttendanceDate);
            } else if ("lateMinutes".equals(request.getSortField())) {
                queryWrapper.orderBy(true, "asc".equalsIgnoreCase(request.getSortOrder()),
                        Attendance::getStatus);
            }
        } else {
            queryWrapper.orderByDesc(Attendance::getAttendanceDate);
        }

        Page<Attendance> page = new Page<>(pageNum, pageSize);
        IPage<Attendance> pageResult = this.page(page, queryWrapper);

        List<HRAttendanceVO> voList = convertToVOList(pageResult.getRecords());
        return PageResult.of(voList, pageResult.getTotal(), pageNum, pageSize);
    }

    @Override
    public HRAttendanceVO getDetail(Long id) {
        Attendance record = this.getById(id);
        ThrowUtils.throwIf(record == null, ErrorCode.NOT_FOUND_ERROR);
        return convertToVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HRAttendanceVO createAttendance(HRAttendanceDTO dto) {
        ThrowUtils.throwIf(dto.getEmployeeId() == null, ErrorCode.PARAMS_ERROR, "员工不能为空");
        ThrowUtils.throwIf(dto.getAttendanceDate() == null, ErrorCode.PARAMS_ERROR, "打卡日期不能为空");

        Employee emp = employeeService.getById(dto.getEmployeeId());
        ThrowUtils.throwIf(emp == null, ErrorCode.NOT_FOUND_ERROR, "员工不存在");

        String dateStr = DateUtil.formatDate(dto.getAttendanceDate());
        Attendance existing = this.lambdaQuery()
                .eq(Attendance::getEmployeeId, dto.getEmployeeId())
                .eq(Attendance::getAttendanceDate, dto.getAttendanceDate())
                .one();
        ThrowUtils.throwIf(existing != null, ErrorCode.OPERATION_ERROR, "该员工当天已存在考勤记录");

        Attendance record = new Attendance();
        record.setEmployeeId(dto.getEmployeeId());
        record.setUserId(emp.getUserId());
        record.setAttendanceDate(dto.getAttendanceDate());
        record.setPunchInTime(dto.getPunchInTime());
        record.setPunchOutTime(dto.getPunchOutTime());
        record.setPunchInLocation(dto.getPunchInLocation());
        record.setPunchOutLocation(dto.getPunchOutLocation());
        record.setOvertimeHours(dto.getOvertimeHours());
        record.setRemark(dto.getRemark());

        calculateAttendanceStatus(record);

        boolean saved = this.save(record);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "保存失败");

        return convertToVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HRAttendanceVO updateAttendance(HRAttendanceDTO dto) {
        ThrowUtils.throwIf(dto.getId() == null, ErrorCode.PARAMS_ERROR, "ID不能为空");

        Attendance record = this.getById(dto.getId());
        ThrowUtils.throwIf(record == null, ErrorCode.NOT_FOUND_ERROR, "考勤记录不存在");

        BeanUtils.copyProperties(dto, record, "id", "employeeId", "attendanceDate",
                "status", "lateMinutes", "earlyMinutes", "overtimeHours");

        if (dto.getPunchInTime() != null) {
            record.setPunchInTime(dto.getPunchInTime());
        }
        if (dto.getPunchOutTime() != null) {
            record.setPunchOutTime(dto.getPunchOutTime());
        }

        calculateAttendanceStatus(record);

        boolean updated = this.updateById(record);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新失败");

        return convertToVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAttendance(Long id) {
        Attendance record = this.getById(id);
        ThrowUtils.throwIf(record == null, ErrorCode.NOT_FOUND_ERROR, "考勤记录不存在");
        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteAttendance(Long[] ids) {
        ThrowUtils.throwIf(ids == null || ids.length == 0, ErrorCode.PARAMS_ERROR, "请选择要删除的记录");
        this.removeByIds(Arrays.asList(ids));
    }

    private void calculateAttendanceStatus(Attendance record) {
        record.setLateMinutes(0);
        record.setEarlyMinutes(0);
        record.setOvertimeHours(0.0);

        if (record.getPunchInTime() == null && record.getPunchOutTime() == null) {
            record.setStatus(AttendanceStatusEnum.ABSENT.getValue());
            return;
        }

        // 获取员工考勤组规则
        AttendanceGroup group = attendanceGroupService.getGroupByEmployeeId(record.getEmployeeId());
        int workStartHour = AttendanceConstant.DEFAULT_WORK_START_HOUR;
        int workEndHour = AttendanceConstant.DEFAULT_WORK_END_HOUR;
        int lateGrace = AttendanceConstant.LATE_GRACE_MINUTES;
        int earlyGrace = AttendanceConstant.LATE_GRACE_MINUTES;

        if (group != null) {
            if (group.getWorkStartTime() != null) {
                workStartHour = DateUtil.hour(group.getWorkStartTime(), true);
            }
            if (group.getWorkEndTime() != null) {
                workEndHour = DateUtil.hour(group.getWorkEndTime(), true);
            }
            if (group.getLateThreshold() != null) {
                lateGrace = group.getLateThreshold();
            }
            if (group.getEarlyThreshold() != null) {
                earlyGrace = group.getEarlyThreshold();
            }
        }

        if (record.getPunchInTime() != null) {
            int punchInHour = DateUtil.hour(record.getPunchInTime(), true);
            int punchInMinute = DateUtil.minute(record.getPunchInTime());
            int thresholdMinutes = workStartHour * 60 + lateGrace;
            int punchInTotalMinutes = punchInHour * 60 + punchInMinute;

            if (punchInTotalMinutes > thresholdMinutes) {
                record.setLateMinutes(punchInTotalMinutes - thresholdMinutes);
            }
        }

        if (record.getPunchOutTime() != null) {
            int punchOutHour = DateUtil.hour(record.getPunchOutTime(), true);
            int punchOutMinute = DateUtil.minute(record.getPunchOutTime());
            int workEndMinutes = workEndHour * 60;
            int punchOutTotalMinutes = punchOutHour * 60 + punchOutMinute;

            if (punchOutTotalMinutes < workEndMinutes - earlyGrace) {
                record.setEarlyMinutes(workEndMinutes - punchOutTotalMinutes);
            } else if (punchOutTotalMinutes > workEndMinutes) {
                record.setOvertimeHours((punchOutTotalMinutes - workEndMinutes) / 60.0);
            }
        }

        if (record.getLateMinutes() > 0) {
            record.setStatus(AttendanceStatusEnum.LATE.getValue());
        } else if (record.getEarlyMinutes() > 0) {
            record.setStatus(AttendanceStatusEnum.LEAVE_EARLY.getValue());
        } else if (record.getPunchInTime() != null && record.getPunchOutTime() != null) {
            record.setStatus(AttendanceStatusEnum.NORMAL.getValue());
        } else {
            record.setStatus(AttendanceStatusEnum.MISSING.getValue());
        }
    }

    private List<HRAttendanceVO> convertToVOList(List<Attendance> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Employee> employeeMap = employeeService.listByIds(
                records.stream().map(Attendance::getEmployeeId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Employee::getId, e -> e));

        Map<Long, Department> deptMap = departmentService.listByIds(
                employeeMap.values().stream().map(Employee::getDepartmentId).filter(Objects::nonNull).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Department::getId, d -> d));

        List<Long> employeeIds = records.stream().map(Attendance::getEmployeeId).distinct().collect(Collectors.toList());
        Map<Long, List<Leave>> leaveMap = new HashMap<>();
        if (!employeeIds.isEmpty()) {
            // 只查询与当前页考勤日期范围相关的请假记录
            Date minDate = records.stream().map(Attendance::getAttendanceDate).min(Date::compareTo).orElse(null);
            Date maxDate = records.stream().map(Attendance::getAttendanceDate).max(Date::compareTo).orElse(null);
            List<Leave> leaves = leaveService.lambdaQuery()
                    .in(Leave::getEmployeeId, employeeIds)
                    .eq(Leave::getStatus, 1)
                    .ge(minDate != null, Leave::getEndDate, minDate)
                    .le(maxDate != null, Leave::getStartDate, maxDate)
                    .list();
            for (Leave leave : leaves) {
                leaveMap.computeIfAbsent(leave.getEmployeeId(), k -> new ArrayList<>()).add(leave);
            }
        }

        return records.stream().map(r -> {
            HRAttendanceVO vo = convertToVO(r);
            Employee emp = employeeMap.get(r.getEmployeeId());
            if (emp != null) {
                vo.setEmployeeNo(emp.getEmployeeNo());
                vo.setEmployeeName(emp.getEmployeeName());
                vo.setDepartmentId(emp.getDepartmentId());
                Department dept = deptMap.get(emp.getDepartmentId());
                if (dept != null) {
                    vo.setDeptName(dept.getDeptName());
                }

                List<Leave> empLeaves = leaveMap.get(r.getEmployeeId());
                if (empLeaves != null) {
                    String dateStr = DateUtil.formatDate(r.getAttendanceDate());
                    for (Leave leave : empLeaves) {
                        if (dateStr.compareTo(DateUtil.formatDate(leave.getStartDate())) >= 0
                                && dateStr.compareTo(DateUtil.formatDate(leave.getEndDate())) <= 0) {
                            LeaveTypeEnum lt = LeaveTypeEnum.getEnumByValue(leave.getLeaveType());
                            vo.setLeaveTypeText(lt != null ? lt.getText() : "无");
                            vo.setStatus(AttendanceStatusEnum.LEAVE.getValue());
                            vo.setStatusText("请假");
                            break;
                        }
                    }
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private HRAttendanceVO convertToVO(Attendance record) {
        HRAttendanceVO vo = new HRAttendanceVO();
        BeanUtils.copyProperties(record, vo);

        if (record.getAttendanceDate() != null) {
            vo.setMonth(DateUtil.format(record.getAttendanceDate(), "yyyy-MM"));
        }

        AttendanceStatusEnum status = AttendanceStatusEnum.getEnumByValue(record.getStatus());
        vo.setStatusText(status != null ? status.getText() : "未知");

        return vo;
    }

    private void applyDataScopeFilter(LambdaQueryWrapper<Attendance> queryWrapper, Long userId, Integer dataScope) {
        DataScopeEnum scope = DataScopeEnum.getByCode(dataScope);
        
        switch (scope) {
            case ALL:
            case ALL_EMPLOYEE:
                break;
            case DEPARTMENT:
                try {
                    Employee manager = employeeService.getByUserId(userId);
                    if (manager != null) {
                        // 查找该主管负责的所有部门
                        List<Long> managedDeptIds = departmentService.lambdaQuery()
                                .eq(Department::getManagerId, manager.getId())
                                .eq(Department::getIsDeleted, 0)
                                .list().stream().map(Department::getId).collect(Collectors.toList());
                        if (!managedDeptIds.isEmpty()) {
                            List<Long> deptEmpIds = employeeService.lambdaQuery()
                                    .in(Employee::getDepartmentId, managedDeptIds)
                                    .list().stream().map(Employee::getId).collect(Collectors.toList());
                            if (!deptEmpIds.isEmpty()) {
                                queryWrapper.in(Attendance::getEmployeeId, deptEmpIds);
                            } else {
                                queryWrapper.eq(Attendance::getEmployeeId, 0L);
                            }
                        } else {
                            // 未负责任何部门，仅看自己
                            queryWrapper.eq(Attendance::getEmployeeId, manager.getId());
                        }
                    }
                } catch (Exception e) {
                    log.warn("获取部门主管信息失败, userId: {}", userId, e);
                }
                break;
            case SELF:
                try {
                    Employee emp = employeeService.getByUserId(userId);
                    if (emp != null) {
                        queryWrapper.eq(Attendance::getEmployeeId, emp.getId());
                    }
                } catch (Exception e) {
                    log.warn("获取员工信息失败, userId: {}", userId, e);
                    queryWrapper.eq(Attendance::getEmployeeId, 0L);
                }
                break;
            default:
                break;
        }
    }
}