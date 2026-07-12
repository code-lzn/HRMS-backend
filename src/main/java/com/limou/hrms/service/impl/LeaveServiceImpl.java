package com.limou.hrms.service.impl;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.AttendanceConstant;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.LeaveMapper;
import com.limou.hrms.model.entity.Attendance;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.Leave;
import com.limou.hrms.model.enums.ApprovalStatusEnum;
import com.limou.hrms.model.enums.LeaveTypeEnum;
import com.limou.hrms.model.vo.LeaveVO;
import com.limou.hrms.service.AttendanceService;
import com.limou.hrms.service.EmployeeService;
import com.limou.hrms.service.LeaveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 请假服务实现
 */
@Service
@Slf4j
public class LeaveServiceImpl extends ServiceImpl<LeaveMapper, Leave>
        implements LeaveService {

    @Resource
    private EmployeeService employeeService;

    @Resource
    private AttendanceService attendanceService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LeaveVO apply(Long userId, Integer leaveType, String startDate, String endDate, String reason) {
        Employee emp = getEmployee(userId);
        Date start = DateUtil.parseDate(startDate);
        Date end = DateUtil.parseDate(endDate);

        ThrowUtils.throwIf(start.after(end), ErrorCode.PARAMS_ERROR, "开始日期不能晚于结束日期");

        // 计算请假天数
        long days = DateUtil.between(start, end, DateUnit.DAY, false) + 1;
        ThrowUtils.throwIf(days <= 0, ErrorCode.PARAMS_ERROR, "请假天数不正确");

        // 检查是否有重叠的请假记录
        long overlapCount = this.lambdaQuery()
                .eq(Leave::getEmployeeId, emp.getId())
                .ne(Leave::getStatus, AttendanceConstant.APPROVAL_STATUS_REJECTED)
                .ne(Leave::getStatus, AttendanceConstant.APPROVAL_STATUS_CANCELLED)
                .le(Leave::getStartDate, end)
                .ge(Leave::getEndDate, start)
                .count();
        ThrowUtils.throwIf(overlapCount > 0, ErrorCode.LEAVE_OVERLAP_ERROR);

        Leave request = new Leave();
        request.setEmployeeId(emp.getId());
        request.setUserId(userId);
        request.setLeaveType(leaveType);
        request.setStartDate(start);
        request.setEndDate(end);
        request.setTotalDays(BigDecimal.valueOf(days));
        request.setReason(reason);
        request.setStatus(AttendanceConstant.APPROVAL_STATUS_PENDING);

        boolean saved = this.save(request);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "请假申请失败");

        // 同步更新考勤记录状态为"请假"
        updateAttendanceForLeave(emp.getId(), start, end);

        return convertToVO(request, emp);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LeaveVO approve(Long requestId, Integer result, String comment, Long approverId) {
        Leave request = this.getById(requestId);
        ThrowUtils.throwIf(request == null, ErrorCode.NOT_FOUND_ERROR, "请假申请不存在");
        ThrowUtils.throwIf(request.getStatus() != AttendanceConstant.APPROVAL_STATUS_PENDING,
                ErrorCode.APPROVAL_NOT_PENDING_ERROR);

        Date now = new Date();
        request.setStatus(result);
        request.setApproverId(approverId);
        request.setApproveTime(now);
        request.setApproveComment(comment);

        boolean updated = this.updateById(request);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "审批失败");

        // 如果拒绝，还原考勤状态
        if (Objects.equals(result, AttendanceConstant.APPROVAL_STATUS_REJECTED)) {
            revertAttendanceForLeave(request.getEmployeeId(), request.getStartDate(), request.getEndDate());
        }

        Employee emp = employeeService.lambdaQuery().eq(Employee::getId, request.getEmployeeId()).one();
        return convertToVO(request, emp);
    }

    @Override
    public List<LeaveVO> getMyLeaves(Long userId) {
        Employee emp = getEmployee(userId);
        List<Leave> list = this.lambdaQuery()
                .eq(Leave::getEmployeeId, emp.getId())
                .orderByDesc(Leave::getCreateTime)
                .list();

        return list.stream().map(r -> convertToVO(r, emp)).collect(Collectors.toList());
    }

    @Override
    public void cancel(Long requestId, Long userId) {
        Leave request = this.getById(requestId);
        ThrowUtils.throwIf(request == null, ErrorCode.NOT_FOUND_ERROR, "请假申请不存在");
        ThrowUtils.throwIf(!Objects.equals(request.getUserId(), userId), ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(request.getStatus() != AttendanceConstant.APPROVAL_STATUS_PENDING,
                ErrorCode.APPROVAL_NOT_PENDING_ERROR);

        request.setStatus(AttendanceConstant.APPROVAL_STATUS_CANCELLED);
        boolean updated = this.updateById(request);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "撤销失败");

        // 还原考勤状态
        revertAttendanceForLeave(request.getEmployeeId(), request.getStartDate(), request.getEndDate());
    }

    // ========== 私有方法 ==========

    private Employee getEmployee(Long userId) {
        Employee emp = employeeService.lambdaQuery().eq(Employee::getUserId, userId).one();
        ThrowUtils.throwIf(emp == null, ErrorCode.NOT_FOUND_ERROR, "员工档案不存在");
        return emp;
    }

    private LeaveVO convertToVO(Leave request, Employee emp) {
        LeaveVO vo = new LeaveVO();
        BeanUtils.copyProperties(request, vo);
        vo.setEmployeeName(emp != null ? emp.getEmployeeName() : null);
        LeaveTypeEnum leaveType = LeaveTypeEnum.getEnumByValue(request.getLeaveType());
        vo.setLeaveTypeText(leaveType != null ? leaveType.getText() : "未知");
        ApprovalStatusEnum status = ApprovalStatusEnum.getEnumByValue(request.getStatus());
        vo.setStatusText(status != null ? status.getText() : "未知");
        return vo;
    }

    /**
     * 请假通过后，更新考勤记录状态为"请假"
     */
    private void updateAttendanceForLeave(Long employeeId, Date start, Date end) {
        Date cursor = start;
        while (!cursor.after(end)) {
            String dateStr = DateUtil.formatDate(cursor);
            Attendance record = attendanceService.lambdaQuery()
                    .eq(Attendance::getEmployeeId, employeeId)
                    .eq(Attendance::getAttendanceDate, DateUtil.parseDate(dateStr))
                    .one();
            if (record == null) {
                record = new Attendance();
                record.setEmployeeId(employeeId);
                record.setAttendanceDate(DateUtil.parseDate(dateStr));
                record.setStatus(AttendanceConstant.ATTENDANCE_STATUS_LEAVE);
                attendanceService.save(record);
            } else {
                record.setStatus(AttendanceConstant.ATTENDANCE_STATUS_LEAVE);
                attendanceService.updateById(record);
            }
            cursor = DateUtil.offsetDay(cursor, 1);
        }
    }

    /**
     * 请假拒绝/撤销后，还原考勤状态
     */
    private void revertAttendanceForLeave(Long employeeId, Date start, Date end) {
        Date cursor = start;
        while (!cursor.after(end)) {
            String dateStr = DateUtil.formatDate(cursor);
            Attendance record = attendanceService.lambdaQuery()
                    .eq(Attendance::getEmployeeId, employeeId)
                    .eq(Attendance::getAttendanceDate, DateUtil.parseDate(dateStr))
                    .one();
            if (record != null && record.getStatus() == AttendanceConstant.ATTENDANCE_STATUS_LEAVE) {
                // 还原：如果有打卡记录就保留原始状态，否则标记缺卡
                if (record.getPunchInTime() != null || record.getPunchOutTime() != null) {
                    record.setStatus(AttendanceConstant.ATTENDANCE_STATUS_NORMAL);
                } else {
                    record.setStatus(AttendanceConstant.ATTENDANCE_STATUS_MISSING);
                }
                attendanceService.updateById(record);
            }
            cursor = DateUtil.offsetDay(cursor, 1);
        }
    }
}
