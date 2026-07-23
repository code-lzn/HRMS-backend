package com.limou.hrms.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.OvertimeRecordMapper;
import com.limou.hrms.model.entity.ApprovalDetail;
import com.limou.hrms.model.entity.ApprovalRecord;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.OvertimeRecord;
import com.limou.hrms.model.enums.ApprovalActionEnum;
import com.limou.hrms.model.enums.ApprovalRecordStatusEnum;
import com.limou.hrms.model.enums.ApprovalStatusEnum;
import com.limou.hrms.model.enums.BusinessTypeEnum;
import com.limou.hrms.model.enums.ProgressNodeStatusEnum;
import com.limou.hrms.model.vo.OvertimeProgressVO;
import com.limou.hrms.model.vo.OvertimeVO;
import com.limou.hrms.service.ApprovalDetailService;
import com.limou.hrms.service.ApprovalService;
import com.limou.hrms.service.EmployeeService;
import com.limou.hrms.service.OvertimeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OvertimeServiceImpl extends ServiceImpl<OvertimeRecordMapper, OvertimeRecord>
        implements OvertimeService {

    private static final String[] OVERTIME_TYPE_TEXTS = {"工作日加班", "休息日加班", "节假日加班"};

    @Resource
    private EmployeeService employeeService;

    @Resource
    private ApprovalService approvalService;

    @Resource
    private ApprovalDetailService approvalDetailService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OvertimeVO apply(Long userId, String overtimeDate, String startTime, String endTime,
                            Double overtimeHours, Integer overtimeType, String reason) {
        Employee emp = getEmployee(userId);

        Date start = DateUtil.parseDateTime(overtimeDate + " " + startTime + ":00");
        Date end = DateUtil.parseDateTime(overtimeDate + " " + endTime + ":00");
        ThrowUtils.throwIf(start.after(end), ErrorCode.PARAMS_ERROR, "开始时间不能晚于结束时间");
        ThrowUtils.throwIf(overtimeHours == null || overtimeHours <= 0,
                ErrorCode.PARAMS_ERROR, "加班时长必须大于0");

        OvertimeRecord record = new OvertimeRecord();
        record.setEmployeeId(emp.getId());
        record.setUserId(userId);
        record.setOvertimeDate(DateUtil.parseDate(overtimeDate));
        record.setStartTime(start);
        record.setEndTime(end);
        record.setOvertimeHours(BigDecimal.valueOf(overtimeHours).setScale(1, RoundingMode.HALF_UP));
        record.setOvertimeType(overtimeType != null ? overtimeType : 0);
        record.setReason(reason);
        record.setStatus(ApprovalStatusEnum.PENDING.getValue());

        boolean saved = this.save(record);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "加班申请失败");

        approvalService.startApproval(
                BusinessTypeEnum.OVERTIME.getValue(), record.getId(),
                emp.getId(), emp.getEmployeeName());

        return convertToVO(record, emp);
    }

    @Override
    public List<OvertimeVO> getMyOvertimes(Long userId) {
        Employee emp = getEmployee(userId);
        List<OvertimeRecord> list = this.lambdaQuery()
                .eq(OvertimeRecord::getEmployeeId, emp.getId())
                .ne(OvertimeRecord::getStatus, ApprovalStatusEnum.CANCELLED.getValue())
                .orderByDesc(OvertimeRecord::getCreateTime)
                .list();
        return list.stream().map(r -> convertToVO(r, emp)).collect(Collectors.toList());
    }

    @Override
    public void cancel(Long requestId, Long userId) {
        OvertimeRecord record = this.getById(requestId);
        ThrowUtils.throwIf(record == null, ErrorCode.NOT_FOUND_ERROR, "加班申请不存在");
        ThrowUtils.throwIf(!Objects.equals(record.getUserId(), userId), ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(!Objects.equals(record.getStatus(), ApprovalStatusEnum.PENDING.getValue()),
                ErrorCode.APPROVAL_NOT_PENDING_ERROR);

        record.setStatus(ApprovalStatusEnum.CANCELLED.getValue());
        this.updateById(record);

        ApprovalRecord approvalRecord = approvalService.lambdaQuery()
                .eq(ApprovalRecord::getBusinessType, BusinessTypeEnum.OVERTIME.getValue())
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

    @Override
    public OvertimeProgressVO getApprovalProgress(Long requestId, Long userId) {
        OvertimeRecord record = this.getById(requestId);
        ThrowUtils.throwIf(record == null, ErrorCode.NOT_FOUND_ERROR, "加班申请不存在");
        ThrowUtils.throwIf(!Objects.equals(record.getUserId(), userId), ErrorCode.NO_AUTH_ERROR);

        Employee emp = employeeService.lambdaQuery().eq(Employee::getId, record.getEmployeeId()).one();
        OvertimeVO overtimeVO = convertToVO(record, emp);

        List<OvertimeProgressVO.ProgressNode> nodes = new ArrayList<>();

        // 节点0: 提交申请
        OvertimeProgressVO.ProgressNode submitNode = new OvertimeProgressVO.ProgressNode();
        submitNode.setNodeName("提交申请");
        submitNode.setStatus(ProgressNodeStatusEnum.COMPLETED.getValue());
        submitNode.setOperatorName(emp != null ? emp.getEmployeeName() : null);
        submitNode.setOperateTime(record.getCreateTime());
        submitNode.setComment(record.getReason());
        nodes.add(submitNode);

        // 查询审批实例
        ApprovalRecord approvalRecord = approvalService.lambdaQuery()
                .eq(ApprovalRecord::getBusinessType, BusinessTypeEnum.OVERTIME.getValue())
                .eq(ApprovalRecord::getBusinessId, requestId)
                .orderByDesc(ApprovalRecord::getCreateTime)
                .last("LIMIT 1")
                .one();

        if (approvalRecord != null) {
            List<ApprovalDetail> details = approvalDetailService.lambdaQuery()
                    .eq(ApprovalDetail::getRecordId, approvalRecord.getId())
                    .orderByAsc(ApprovalDetail::getStepOrder, ApprovalDetail::getCreateTime)
                    .list();

            for (ApprovalDetail detail : details) {
                OvertimeProgressVO.ProgressNode node = new OvertimeProgressVO.ProgressNode();
                node.setNodeName(detail.getNodeName());

                String operatorName = detail.getApproverName();
                if (operatorName == null && detail.getApproverId() != null) {
                    Employee approver = employeeService.getById(detail.getApproverId());
                    operatorName = approver != null ? approver.getEmployeeName() : null;
                }
                if (operatorName == null) {
                    operatorName = "待分配";
                }
                node.setOperatorName(operatorName);
                node.setOperateTime(detail.getOperateTime());
                node.setComment(detail.getComment());

                String action = detail.getAction();
                if (ApprovalActionEnum.PENDING.getValue().equals(action)) {
                    boolean isCurrentStep = Objects.equals(detail.getStepOrder(), approvalRecord.getCurrentStep())
                            && ApprovalRecordStatusEnum.APPROVING.getValue().equals(approvalRecord.getStatus());
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
            OvertimeProgressVO.ProgressNode fallbackNode = new OvertimeProgressVO.ProgressNode();
            fallbackNode.setNodeName("审批");
            fallbackNode.setStatus(ProgressNodeStatusEnum.NOT_STARTED.getValue());
            fallbackNode.setOperatorName(null);
            fallbackNode.setOperateTime(null);
            fallbackNode.setComment("审批流未初始化");
            nodes.add(fallbackNode);
        }

        OvertimeProgressVO vo = new OvertimeProgressVO();
        vo.setOvertime(overtimeVO);
        vo.setProgressNodes(nodes);
        return vo;
    }

    private Employee getEmployee(Long userId) {
        Employee emp = employeeService.lambdaQuery().eq(Employee::getUserId, userId).one();
        ThrowUtils.throwIf(emp == null, ErrorCode.NOT_FOUND_ERROR, "员工档案不存在");
        return emp;
    }

    private OvertimeVO convertToVO(OvertimeRecord record, Employee emp) {
        OvertimeVO vo = new OvertimeVO();
        BeanUtils.copyProperties(record, vo);
        vo.setEmployeeName(emp != null ? emp.getEmployeeName() : null);
        int type = record.getOvertimeType() != null ? record.getOvertimeType() : 0;
        vo.setOvertimeTypeText(type >= 0 && type < OVERTIME_TYPE_TEXTS.length
                ? OVERTIME_TYPE_TEXTS[type] : "未知");
        ApprovalStatusEnum status = ApprovalStatusEnum.getEnumByValue(record.getStatus());
        vo.setStatusText(status != null ? status.getText() : "未知");
        return vo;
    }
}
