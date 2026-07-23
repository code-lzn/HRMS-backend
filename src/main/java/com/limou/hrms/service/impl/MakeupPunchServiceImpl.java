package com.limou.hrms.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.AttendanceConstant;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.MakeupPunchMapper;
import com.limou.hrms.model.entity.ApprovalDetail;
import com.limou.hrms.model.entity.ApprovalRecord;
import com.limou.hrms.model.entity.Attendance;
import com.limou.hrms.model.entity.AttendanceGroup;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.MakeupPunch;
import com.limou.hrms.model.enums.*;
import com.limou.hrms.model.vo.MakeupPunchProgressVO;
import com.limou.hrms.model.vo.MakeupPunchVO;
import com.limou.hrms.service.ApprovalDetailService;
import com.limou.hrms.service.ApprovalService;
import com.limou.hrms.service.AttendanceGroupService;
import com.limou.hrms.service.AttendanceService;
import com.limou.hrms.service.EmployeeService;
import com.limou.hrms.service.MakeupPunchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 补卡服务实现
 */
@Service
@Slf4j
public class MakeupPunchServiceImpl extends ServiceImpl<MakeupPunchMapper, MakeupPunch>
        implements MakeupPunchService {

    @Resource
    private EmployeeService employeeService;

    @Resource
    private AttendanceService attendanceService;

    @Resource
    private AttendanceGroupService attendanceGroupService;

    @Resource
    private ApprovalService approvalService;

    @Resource
    private ApprovalDetailService approvalDetailService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MakeupPunchVO apply(Long userId, String punchDate, Integer punchType,
                                       String punchTime, String reason) {
        Employee emp = getEmployee(userId);

        // 不能补卡未来的日期
        ThrowUtils.throwIf(punchDate.compareTo(DateUtil.formatDate(new Date())) > 0,
                ErrorCode.PARAMS_ERROR, "不能补卡未来的日期");

        // 每月最多2次补卡
        String month = punchDate.substring(0, 7);
        long monthCount = this.lambdaQuery()
                .eq(MakeupPunch::getEmployeeId, emp.getId())
                .ge(MakeupPunch::getPunchDate, DateUtil.parseDate(month + "-01"))
                .le(MakeupPunch::getPunchDate,
                        DateUtil.endOfMonth(DateUtil.parseDate(month + "-01")))
                .count();
        if (monthCount >= 2) {
            ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR,
                    "每月最多申请2次补卡，当前月已申请 " + monthCount + " 次");
        }

        MakeupPunch request = new MakeupPunch();
        request.setEmployeeId(emp.getId());
        request.setUserId(userId);
        request.setPunchDate(DateUtil.parseDate(punchDate));
        request.setPunchType(punchType);
        // punchTime 格式为 "HH:mm"，拼接完整时间
        request.setPunchTime(DateUtil.parseDateTime(punchDate + " " + punchTime + ":00"));
        request.setReason(reason);
        request.setStatus(ApprovalStatusEnum.PENDING.getValue());

        boolean saved = this.save(request);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "补卡申请失败");

        // 同步到审批中心
        approvalService.startApproval(BusinessTypeEnum.PATCH_CLOCK.getValue(), request.getId(), emp.getId(), emp.getEmployeeName());

        return convertToVO(request, emp);
    }

//    @Override
//    @Transactional(rollbackFor = Exception.class)
//    public MakeupPunchVO approve(Long requestId, Integer result, String comment, Long approverId) {
//        MakeupPunch request = this.getById(requestId);
//        ThrowUtils.throwIf(request == null, ErrorCode.NOT_FOUND_ERROR, "补卡申请不存在");
//        ThrowUtils.throwIf(request.getStatus() != ApprovalStatusEnum.PENDING.getValue(),
//                ErrorCode.APPROVAL_NOT_PENDING_ERROR);
//
//        Date now = new Date();
//        request.setStatus(result);
//        request.setApproverId(approverId);
//        request.setApproveTime(now);
//        request.setApproveComment(comment);
//
//        boolean updated = this.updateById(request);
//        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "审批失败");
//
//        // 审批通过后，更新打卡记录
//        if (Objects.equals(result, ApprovalStatusEnum.APPROVED.getValue())) {
//            updateAttendanceRecord(request);
//        }
//
//        Employee emp = employeeService.lambdaQuery().eq(Employee::getId, request.getEmployeeId()).one();
//        return convertToVO(request, emp);
//    }

    @Override
    public List<MakeupPunchVO> getMyMakeupPunches(Long userId) {
        Employee emp = getEmployee(userId);
        List<MakeupPunch> list = this.lambdaQuery()
                .eq(MakeupPunch::getEmployeeId, emp.getId())
                .ne(MakeupPunch::getStatus, ApprovalStatusEnum.CANCELLED.getValue())
                .orderByDesc(MakeupPunch::getCreateTime)
                .list();

        return list.stream().map(r -> convertToVO(r, emp)).collect(Collectors.toList());
    }

    @Override
    public MakeupPunchProgressVO getApprovalProgress(Long requestId, Long userId) {
        MakeupPunch request = this.getById(requestId);
        ThrowUtils.throwIf(request == null, ErrorCode.NOT_FOUND_ERROR, "补卡申请不存在");
        ThrowUtils.throwIf(!Objects.equals(request.getUserId(), userId), ErrorCode.NO_AUTH_ERROR);

        Employee emp = employeeService.lambdaQuery().eq(Employee::getId, request.getEmployeeId()).one();
        MakeupPunchVO punchVO = convertToVO(request, emp);

        List<MakeupPunchProgressVO.ProgressNode> nodes = new ArrayList<>();

        // 节点0: 提交申请
        MakeupPunchProgressVO.ProgressNode submitNode = new MakeupPunchProgressVO.ProgressNode();
        submitNode.setNodeName("提交申请");
        submitNode.setStatus(ProgressNodeStatusEnum.COMPLETED.getValue());
        submitNode.setOperatorName(emp != null ? emp.getEmployeeName() : null);
        submitNode.setOperateTime(request.getCreateTime());
        submitNode.setComment(request.getReason());
        nodes.add(submitNode);

        // 查询审批实例
        ApprovalRecord record = approvalService.lambdaQuery()
                .eq(ApprovalRecord::getBusinessType, BusinessTypeEnum.PATCH_CLOCK.getValue())
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
                MakeupPunchProgressVO.ProgressNode node = new MakeupPunchProgressVO.ProgressNode();
                node.setNodeName(detail.getNodeName());

                String operatorName = detail.getApproverName();
                if (operatorName == null && detail.getApproverId() != null) {
                    Employee approver = employeeService.getById(detail.getApproverId());
                    operatorName = approver != null ? approver.getEmployeeName() : null;
                }
                if (operatorName == null) {
                    operatorName = "limou";
                }
                node.setOperatorName(operatorName);
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
            MakeupPunchProgressVO.ProgressNode fallbackNode = new MakeupPunchProgressVO.ProgressNode();
            fallbackNode.setNodeName("审批");
            fallbackNode.setStatus(ProgressNodeStatusEnum.NOT_STARTED.getValue());
            fallbackNode.setOperatorName(null);
            fallbackNode.setOperateTime(null);
            fallbackNode.setComment("审批流未初始化");
            nodes.add(fallbackNode);
        }

        MakeupPunchProgressVO vo = new MakeupPunchProgressVO();
        vo.setMakeupPunch(punchVO);
        vo.setProgressNodes(nodes);
        return vo;
    }

    @Override
    public void cancel(Long requestId, Long userId) {
        MakeupPunch request = this.getById(requestId);
        ThrowUtils.throwIf(request == null, ErrorCode.NOT_FOUND_ERROR, "补卡申请不存在");
        ThrowUtils.throwIf(!Objects.equals(request.getUserId(), userId), ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(!Objects.equals(request.getStatus(), ApprovalStatusEnum.PENDING.getValue()),
                ErrorCode.APPROVAL_NOT_PENDING_ERROR);

        request.setStatus(ApprovalStatusEnum.CANCELLED.getValue());
        boolean updated = this.updateById(request);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "撤回失败");

        // 同步到审批中心
        ApprovalRecord approvalRecord = approvalService.lambdaQuery()
                .eq(ApprovalRecord::getBusinessType, BusinessTypeEnum.PATCH_CLOCK.getValue())
                .eq(ApprovalRecord::getBusinessId, requestId)
                .one();
        if (approvalRecord != null) {
            approvalRecord.setStatus(ApprovalRecordStatusEnum.WITHDRAWN.getValue());
            approvalRecord.setFinishedAt(new Date());
            approvalService.updateById(approvalRecord);
            List<ApprovalDetail> details = approvalDetailService.lambdaQuery()
                    .eq(ApprovalDetail::getRecordId, approvalRecord.getId())
                    .eq(ApprovalDetail::getAction, ApprovalActionEnum.PENDING.getValue())
                    .list();
            for (ApprovalDetail detail : details) {
                detail.setAction(ApprovalActionEnum.WITHDRAWN.getValue());
                detail.setComment("申请人已撤销");
                detail.setOperateTime(new Date());
                approvalDetailService.updateById(detail);
            }
        }
    }

    // ========== 私有方法 ==========

    private Employee getEmployee(Long userId) {
        Employee emp = employeeService.lambdaQuery().eq(Employee::getUserId, userId).one();
        ThrowUtils.throwIf(emp == null, ErrorCode.NOT_FOUND_ERROR, "员工档案不存在");
        return emp;
    }

    private MakeupPunchVO convertToVO(MakeupPunch request, Employee emp) {
        MakeupPunchVO vo = new MakeupPunchVO();
        BeanUtils.copyProperties(request, vo);
        vo.setEmployeeName(emp != null ? emp.getEmployeeName() : null);
        MakeupPunchTypeEnum punchType = MakeupPunchTypeEnum.getEnumByValue(request.getPunchType());
        vo.setPunchTypeText(punchType != null ? punchType.getText() : "未知");
        ApprovalStatusEnum status = ApprovalStatusEnum.getEnumByValue(request.getStatus());
        vo.setStatusText(status != null ? status.getText() : "未知");
        return vo;
    }

    /**
     * 补卡通过后，更新打卡记录
     */
    private void updateAttendanceRecord(MakeupPunch makeup) {
        Attendance record = attendanceService.lambdaQuery()
                .eq(Attendance::getEmployeeId, makeup.getEmployeeId())
                .eq(Attendance::getAttendanceDate, makeup.getPunchDate())
                .one();

        if (record == null) {
            record = new Attendance();
            record.setEmployeeId(makeup.getEmployeeId());
            record.setUserId(makeup.getUserId());
            record.setAttendanceDate(makeup.getPunchDate());
            record.setStatus(AttendanceStatusEnum.NORMAL.getValue());
            record.setPunchInType(PunchTypeEnum.WEB.getValue());
            record.setPunchOutType(PunchTypeEnum.WEB.getValue());
        }

        if (Objects.equals(makeup.getPunchType(), MakeupPunchTypeEnum.PUNCH_IN.getValue())) {
            record.setPunchInTime(makeup.getPunchTime());
        } else {
            record.setPunchOutTime(makeup.getPunchTime());
        }

        // 重新计算迟到/早退/加班状态
        recalculateStatus(record);

        attendanceService.saveOrUpdate(record);
    }

    private void recalculateStatus(Attendance record) {
        record.setLateMinutes(0);
        record.setEarlyMinutes(0);
        record.setOvertimeHours(0.0);

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
}
