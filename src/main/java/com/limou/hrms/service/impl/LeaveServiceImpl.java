package com.limou.hrms.service.impl;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.AttendanceConstant;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.EmployeeDetailMapper;
import com.limou.hrms.mapper.LeaveMapper;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.ApprovalActionEnum;
import com.limou.hrms.model.enums.ApprovalRecordStatusEnum;
import com.limou.hrms.model.enums.ApprovalStatusEnum;
import com.limou.hrms.model.enums.AttendanceStatusEnum;
import com.limou.hrms.model.enums.BusinessTypeEnum;
import com.limou.hrms.model.enums.LeaveFlowEnum;
import com.limou.hrms.model.enums.LeaveTypeEnum;
import com.limou.hrms.model.enums.ProgressNodeStatusEnum;
import com.limou.hrms.model.entity.ApprovalFlow;
import com.limou.hrms.model.vo.LeaveBalanceVO;
import com.limou.hrms.model.vo.LeaveProgressVO;
import com.limou.hrms.model.vo.LeaveVO;
import com.limou.hrms.service.ApprovalDetailService;
import com.limou.hrms.service.ApprovalFlowService;
import com.limou.hrms.service.ApprovalService;
import com.limou.hrms.service.AttendanceService;
import com.limou.hrms.service.EmployeeLeaveBalanceService;
import com.limou.hrms.service.EmployeeService;
import com.limou.hrms.service.LeaveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
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

    @Resource
    private ApprovalFlowService approvalFlowService;

    @Resource
    private EmployeeLeaveBalanceService employeeLeaveBalanceService;

    @Resource
    private ApprovalDetailService approvalDetailService;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public LeaveVO apply(Long userId, Integer leaveType, String startDate, String endDate, String reason) {
        Employee emp = getEmployee(userId);
        Date start = DateUtil.parseDate(startDate);
        Date end = DateUtil.parseDate(endDate);

        ThrowUtils.throwIf(start.after(end), ErrorCode.PARAMS_ERROR, "开始日期不能晚于结束日期");

        // 计算请假天数（基于工作时间 9:00-18:00，最少0.5天）
        double days = calculateLeaveDays(start, end);

        // 年假/病假/调休校验余额
        checkLeaveBalance(emp.getId(), leaveType, BigDecimal.valueOf(days));

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

        // 同步到审批中心 — 按请假类型+天数选择审批流
        LeaveFlowEnum flowEnum = selectLeaveFlow(LeaveTypeEnum.getEnumByValue(leaveType), days);
        //找到对应的审批流
        ApprovalFlow flow = approvalFlowService.lambdaQuery()
                .eq(ApprovalFlow::getBusinessType, BusinessTypeEnum.LEAVE.getValue())
                .eq(ApprovalFlow::getFlowName, flowEnum.getFlowName())
                .eq(ApprovalFlow::getStatus, 1)
                .one();
        if (flow == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "审批流未配置: " + flowEnum.getFlowName());
        }
        approvalService.startApprovalByFlowId(flow.getId(), BusinessTypeEnum.LEAVE.getValue(),
                request.getId(), emp.getId(), emp.getEmployeeName());

        // 同步更新考勤记录状态为"请假"
        updateAttendanceForLeave(emp.getId(), start, end);

        return convertToVO(request, emp);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LeaveVO approve(Long requestId, Integer result, String comment, Long approverId) {
        Leave request = this.getById(requestId);
        ThrowUtils.throwIf(request == null, ErrorCode.NOT_FOUND_ERROR, "请假申请不存在");
        ThrowUtils.throwIf(!Objects.equals(request.getStatus(), ApprovalStatusEnum.PENDING.getValue()),
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

        List<LeaveProgressVO.ProgressNode> nodes = new ArrayList<>();

        // 节点0: 提交申请
        LeaveProgressVO.ProgressNode submitNode = new LeaveProgressVO.ProgressNode();
        submitNode.setNodeName("提交申请");
        submitNode.setStatus(ProgressNodeStatusEnum.COMPLETED.getValue());
        submitNode.setOperatorName(emp != null ? emp.getEmployeeName() : null);
        submitNode.setOperateTime(request.getCreateTime());
        submitNode.setComment(request.getReason());
        nodes.add(submitNode);

        // 查询审批实例
        ApprovalRecord record = approvalService.lambdaQuery()
                .eq(ApprovalRecord::getBusinessType, BusinessTypeEnum.LEAVE.getValue())
                .eq(ApprovalRecord::getBusinessId, requestId)
                .orderByDesc(ApprovalRecord::getCreateTime)
                .last("LIMIT 1")
                .one();

        if (record != null) {
            List<ApprovalDetail> details = approvalDetailService.lambdaQuery()
                    .eq(ApprovalDetail::getRecordId, record.getId())
                    .orderByAsc(ApprovalDetail::getStepOrder, ApprovalDetail::getCreateTime)
                    .list();

            for (ApprovalDetail detail : details) {
                LeaveProgressVO.ProgressNode node = new LeaveProgressVO.ProgressNode();
                node.setNodeName(detail.getNodeName());
                node.setOperatorName(detail.getApproverName());
                node.setOperateTime(detail.getOperateTime());
                node.setComment(detail.getComment());

                String action = detail.getAction();
                if (ApprovalActionEnum.PENDING.getValue().equals(action)) {
                    boolean isCurrentStep = Objects.equals(detail.getStepOrder(), record.getCurrentStep())
                            && ApprovalRecordStatusEnum.APPROVING.getValue().equals(record.getStatus());
                    node.setStatus(isCurrentStep
                            ? ProgressNodeStatusEnum.IN_PROGRESS.getValue()
                            : ProgressNodeStatusEnum.NOT_STARTED.getValue());
                } else if (ApprovalActionEnum.APPROVE.getValue().equals(action)) {
                    node.setStatus(ProgressNodeStatusEnum.COMPLETED.getValue());
                } else if (ApprovalActionEnum.REJECT.getValue().equals(action)) {
                    node.setStatus(ProgressNodeStatusEnum.COMPLETED.getValue());
                    node.setNodeName(detail.getNodeName() + "（已拒绝）");
                } else if (ApprovalActionEnum.TRANSFER.getValue().equals(action)) {
                    node.setStatus(ProgressNodeStatusEnum.COMPLETED.getValue());
                    node.setNodeName(detail.getNodeName() + "（已转交）");
                } else if (ApprovalActionEnum.WITHDRAWN.getValue().equals(action)) {
                    node.setStatus(ProgressNodeStatusEnum.NOT_STARTED.getValue());
                } else {
                    node.setStatus(ProgressNodeStatusEnum.NOT_STARTED.getValue());
                }

                nodes.add(node);
            }
        } else {
            // 无审批记录时回退
            LeaveProgressVO.ProgressNode fallbackNode = new LeaveProgressVO.ProgressNode();
            fallbackNode.setNodeName("审批");
            fallbackNode.setStatus(ProgressNodeStatusEnum.NOT_STARTED.getValue());
            fallbackNode.setOperatorName(null);
            fallbackNode.setOperateTime(null);
            fallbackNode.setComment("审批流未初始化");
            nodes.add(fallbackNode);
        }

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
        ThrowUtils.throwIf(!Objects.equals(request.getStatus(), ApprovalStatusEnum.PENDING.getValue()),
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
            // 将所有待审批的明细标记为已撤销，防止审批人继续操作
            List<ApprovalDetail> details = approvalDetailService.lambdaQuery()
                    .eq(ApprovalDetail::getRecordId, approvaRecord.getId())
                    .eq(ApprovalDetail::getAction, ApprovalActionEnum.PENDING.getValue())
                    .list();
            for (ApprovalDetail detail : details) {
                detail.setAction(ApprovalActionEnum.WITHDRAWN.getValue());
                detail.setComment("申请人已撤销");
                detail.setOperateTime(new Date());
                approvalDetailService.updateById(detail);
            }
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

    private void checkLeaveBalance(Long employeeId, Integer leaveType, BigDecimal days) {
        if (leaveType == null || days == null) return;
        LeaveBalanceVO balance = employeeLeaveBalanceService.getCurrentYearBalance(employeeId);

        String label;
        BigDecimal remaining;
        switch (leaveType) {
            case 1:
                label = "病假";
                remaining = balance.getSickRemaining();
                break;
            case 2:
                label = "年假";
                remaining = balance.getAnnualRemaining();
                break;
            case 6:
                label = "调休";
                remaining = balance.getCompRemaining();
                break;
            default:
                return;
        }

        ThrowUtils.throwIf(remaining.compareTo(days) < 0,
                ErrorCode.PARAMS_ERROR, label + "余额不足，剩余 " + remaining + " 天，申请 " + days + " 天");
    }

    /**
     * 根据请假类型和天数选择审批流
     */
    private LeaveFlowEnum selectLeaveFlow(LeaveTypeEnum leaveType, double days) {
        if (leaveType == null) return LeaveFlowEnum.SIMPLE;
        switch (leaveType) {
            case ANNUAL:
            case COMPENSATORY:
                return days <= 3 ? LeaveFlowEnum.SIMPLE : LeaveFlowEnum.DEPT;
            case PERSONAL:
            case SICK:
                return days <= 1 ? LeaveFlowEnum.SIMPLE : LeaveFlowEnum.DEPT;
            case MARRIAGE:
            case MATERNITY:
            case FUNERAL:
                return LeaveFlowEnum.HR;
            default:
                return LeaveFlowEnum.SIMPLE;
        }
    }

    /**
     * 计算请假天数，仅统计工作时间 (9:00-18:00) 内的重叠小时数
     */
    private double calculateLeaveDays(Date start, Date end) {
        final int WORK_START = AttendanceConstant.DEFAULT_WORK_START_HOUR; // 9
        final int WORK_END = AttendanceConstant.DEFAULT_WORK_END_HOUR;     // 18
        final double WORK_HOURS_PER_DAY = WORK_END - WORK_START;           // 9

        long totalWorkMinutes = 0;
        Calendar cursor = DateUtil.beginOfDay(start).toCalendar();
        cursor.set(Calendar.MILLISECOND, 0);
        Date endDate = end;

        while (!cursor.getTime().after(endDate)) {
            Date dayStart = cursor.getTime();
            Date dayEnd = DateUtil.endOfDay(dayStart);

            // 当天的工作时间段
            Date workStart = DateUtil.offsetHour(dayStart, WORK_START);
            Date workEnd = DateUtil.offsetHour(dayStart, WORK_END);

            // 当天请假区间 ∩ 工作时间
            Date segStart = start.after(dayStart) ? start : dayStart;
            Date segEnd = end.before(dayEnd) ? end : dayEnd;
            Date overlapStart = segStart.after(workStart) ? segStart : workStart;
            Date overlapEnd = segEnd.before(workEnd) ? segEnd : workEnd;

            if (overlapStart.before(overlapEnd)) {
                totalWorkMinutes += DateUtil.between(overlapStart, overlapEnd, DateUnit.MINUTE);
            }

            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }

        double days = totalWorkMinutes / 60.0 / WORK_HOURS_PER_DAY;
        return Math.max(days, 0.5);
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
            if (record != null && Objects.equals(record.getStatus(), AttendanceStatusEnum.LEAVE.getValue())) {
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
