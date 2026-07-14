package com.limou.hrms.service.impl;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.AttendanceConstant;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.EmployeeDetailMapper;
import com.limou.hrms.mapper.LeaveMapper;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.ApprovalRecordStatusEnum;
import com.limou.hrms.model.enums.ApprovalStatusEnum;
import com.limou.hrms.model.enums.AttendanceStatusEnum;
import com.limou.hrms.model.enums.BusinessTypeEnum;
import com.limou.hrms.model.enums.LeaveTypeEnum;
import com.limou.hrms.model.enums.ProgressNodeStatusEnum;
import com.limou.hrms.model.vo.LeaveProgressVO;
import com.limou.hrms.model.vo.LeaveVO;
import com.limou.hrms.service.AttendanceService;
import com.limou.hrms.service.EmployeeService;
import com.limou.hrms.service.LeaveService;
import com.limou.hrms.service.ApprovalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
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

    @Resource
    private EmployeeDetailMapper employeeDetailMapper;

    @Resource
    private ApprovalService approvalService;


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
                .ne(Leave::getStatus, ApprovalStatusEnum.REJECTED.getValue())
                .ne(Leave::getStatus, ApprovalStatusEnum.CANCELLED.getValue())
                .le(Leave::getStartDate, end)
                .ge(Leave::getEndDate, start)
                .count();
        ThrowUtils.throwIf(overlapCount > 0, ErrorCode.LEAVE_OVERLAP_ERROR);
        //去detail表查询审批人id
        EmployeeDetail employeeDetail = employeeDetailMapper.selectById(emp.getId());

        Leave request = new Leave();
        request.setEmployeeId(emp.getId());
        request.setUserId(userId);
        request.setLeaveType(leaveType);
        request.setStartDate(start);
        request.setEndDate(end);
        request.setTotalDays(BigDecimal.valueOf(days));
        request.setReason(reason);
        request.setApproverId(employeeDetail.getDirectReportId());
        request.setStatus(ApprovalStatusEnum.PENDING.getValue());

        boolean saved = this.save(request);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "请假申请失败");

        // 同步到审批中心
        approvalService.startApproval(BusinessTypeEnum.LEAVE.getValue(), request.getId(), emp.getId(), emp.getEmployeeName());

        // 同步更新考勤记录状态为"请假"
        updateAttendanceForLeave(emp.getId(), start, end);

        return convertToVO(request, emp);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LeaveVO approve(Long requestId, Integer result, String comment, Long approverId) {
        Leave request = this.getById(requestId);
        ThrowUtils.throwIf(request == null, ErrorCode.NOT_FOUND_ERROR, "请假申请不存在");
        ThrowUtils.throwIf(request.getStatus() != ApprovalStatusEnum.PENDING.getValue(),
                ErrorCode.APPROVAL_NOT_PENDING_ERROR);

        Date now = new Date();
        request.setStatus(result);
        request.setApproverId(approverId);
        request.setApproveTime(now);
        request.setApproveComment(comment);

        boolean updated = this.updateById(request);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "审批失败");

        // 如果拒绝，还原考勤状态
        if (Objects.equals(result, ApprovalStatusEnum.REJECTED.getValue())) {
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
    public LeaveProgressVO getApprovalProgress(Long requestId, Long userId) {
        Leave request = this.getById(requestId);
        ThrowUtils.throwIf(request == null, ErrorCode.NOT_FOUND_ERROR, "请假申请不存在");
        ThrowUtils.throwIf(!Objects.equals(request.getUserId(), userId), ErrorCode.NO_AUTH_ERROR);

        Employee emp = employeeService.lambdaQuery().eq(Employee::getId, request.getEmployeeId()).one();
        LeaveVO leaveVO = convertToVO(request, emp);

        // 构建审批进度节点
        List<LeaveProgressVO.ProgressNode> nodes = new ArrayList<>();

        // 节点1: 提交申请
        LeaveProgressVO.ProgressNode submitNode = new LeaveProgressVO.ProgressNode();
        submitNode.setNodeName("提交申请");
        submitNode.setStatus(ProgressNodeStatusEnum.COMPLETED.getValue());
        submitNode.setOperatorName(emp != null ? emp.getEmployeeName() : null);
        submitNode.setOperateTime(request.getCreateTime());
        submitNode.setComment(request.getReason());
        nodes.add(submitNode);

        // 节点2: 审批结果
        LeaveProgressVO.ProgressNode approveNode = new LeaveProgressVO.ProgressNode();
        approveNode.setNodeName("审批");
        if (request.getStatus() == ApprovalStatusEnum.PENDING.getValue()) {
            approveNode.setStatus(ProgressNodeStatusEnum.IN_PROGRESS.getValue());
            approveNode.setOperatorName(null);
            approveNode.setOperateTime(null);
            approveNode.setComment(null);
        } else if (request.getStatus() == ApprovalStatusEnum.CANCELLED.getValue()) {
            approveNode.setStatus(ProgressNodeStatusEnum.NOT_STARTED.getValue());
            approveNode.setNodeName("审批（已撤销）");
            approveNode.setOperatorName(null);
            approveNode.setOperateTime(null);
            approveNode.setComment("申请人已撤销此申请");
        } else {
            approveNode.setStatus(ProgressNodeStatusEnum.COMPLETED.getValue());
            Employee approver = request.getApproverId() != null
                    ? employeeService.lambdaQuery().eq(Employee::getId, request.getApproverId()).one()
                    : null;
            approveNode.setOperatorName(approver != null ? approver.getEmployeeName() : null);
            approveNode.setOperateTime(request.getApproveTime());
            approveNode.setComment(request.getApproveComment());
        }
        nodes.add(approveNode);

        LeaveProgressVO vo = new LeaveProgressVO();
        vo.setLeave(leaveVO);
        vo.setProgressNodes(nodes);
        return vo;
    }

    @Override
    public void cancel(Long requestId, Long userId) {
        Leave request = this.getById(requestId);
        ThrowUtils.throwIf(request == null, ErrorCode.NOT_FOUND_ERROR, "请假申请不存在");
        ThrowUtils.throwIf(!Objects.equals(request.getUserId(), userId), ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(request.getStatus() != ApprovalStatusEnum.PENDING.getValue(),
                ErrorCode.APPROVAL_NOT_PENDING_ERROR);

        request.setStatus(ApprovalStatusEnum.CANCELLED.getValue());
        boolean updated = this.updateById(request);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "撤销失败");

        // 同步到审批中心：更新审批记录状态为 WITHDRAWN
        ApprovalRecord approvaRecord = approvalService.lambdaQuery()
                .eq(ApprovalRecord::getBusinessType, BusinessTypeEnum.LEAVE.getValue())
                .eq(ApprovalRecord::getBusinessId, requestId)
                .one();
        if (approvaRecord != null) {
            approvaRecord.setStatus(ApprovalRecordStatusEnum.WITHDRAWN.getValue());
            approvaRecord.setFinishedAt(new Date());
            approvalService.updateById(approvaRecord);
        }

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
                record.setStatus(AttendanceStatusEnum.LEAVE.getValue());
                attendanceService.save(record);
            } else {
                record.setStatus(AttendanceStatusEnum.LEAVE.getValue());
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
            if (record != null && record.getStatus() == AttendanceStatusEnum.LEAVE.getValue()) {
                if (record.getPunchInTime() != null || record.getPunchOutTime() != null) {
                    // 有打卡记录：还原为正常
                    record.setStatus(AttendanceStatusEnum.NORMAL.getValue());
                    attendanceService.updateById(record);
                } else {
                    // 无打卡记录：该记录是请假时创建的，直接删除
                    attendanceService.removeById(record.getId());
                }
            }
            cursor = DateUtil.offsetDay(cursor, 1);
        }
    }
}
