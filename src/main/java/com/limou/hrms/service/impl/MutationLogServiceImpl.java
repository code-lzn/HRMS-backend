package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.limou.hrms.mapper.*;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.vo.MutationLogVO;
import com.limou.hrms.service.EmployeeService;
import com.limou.hrms.service.MutationLogService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MutationLogServiceImpl implements MutationLogService {

    @Resource
    private EmpMutationLogMapper empMutationLogMapper;
    @Resource
    private HrOnboardingMapper hrOnboardingMapper;
    @Resource
    private HrRegularizationMapper hrRegularizationMapper;
    @Resource
    private HrTransferMapper hrTransferMapper;
    @Resource
    private HrResignationMapper hrResignationMapper;
    @Resource
    private ApprovalDetailMapper approvalDetailMapper;
    @Resource
    private ApprovalRecordMapper approvalRecordMapper;
    @Resource
    private EmployeeService employeeService;

    @Override
    public List<MutationLogVO> getMyMutationLogs(Long userId) {
        Employee emp = employeeService.getByUserId(userId);
        if (emp == null) {
            return new ArrayList<>();
        }

        Set<Long> logIds = new LinkedHashSet<>();

        // 1. 员工作为异动当事人
        List<EmpMutationLog> ownLogs = empMutationLogMapper.selectList(
                new LambdaQueryWrapper<EmpMutationLog>()
                        .eq(EmpMutationLog::getEmployeeId, emp.getId()));
        ownLogs.forEach(log -> logIds.add(log.getId()));

        // 2. 员工作为发起人（HR）
        List<EmpMutationLog> operatorLogs = empMutationLogMapper.selectList(
                new LambdaQueryWrapper<EmpMutationLog>()
                        .eq(EmpMutationLog::getOperatorId, emp.getId()));
        operatorLogs.forEach(log -> logIds.add(log.getId()));

        // 3. 员工作为审批人
        List<ApprovalDetail> approverDetails = approvalDetailMapper.selectList(
                new LambdaQueryWrapper<ApprovalDetail>()
                        .eq(ApprovalDetail::getApproverId, emp.getId()));
        if (!approverDetails.isEmpty()) {
            List<Long> recordIds = approverDetails.stream()
                    .map(ApprovalDetail::getRecordId).distinct().collect(Collectors.toList());
            List<ApprovalRecord> approvalRecords = approvalRecordMapper.selectList(
                    new LambdaQueryWrapper<ApprovalRecord>()
                            .in(ApprovalRecord::getId, recordIds));
            for (ApprovalRecord ar : approvalRecords) {
                List<EmpMutationLog> approverLogs = empMutationLogMapper.selectList(
                        new LambdaQueryWrapper<EmpMutationLog>()
                                .eq(EmpMutationLog::getBusinessType, ar.getBusinessType())
                                .eq(EmpMutationLog::getBusinessId, ar.getBusinessId()));
                approverLogs.forEach(log -> logIds.add(log.getId()));
            }
        }

        if (logIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<EmpMutationLog> allLogs = empMutationLogMapper.selectList(
                new LambdaQueryWrapper<EmpMutationLog>()
                        .in(EmpMutationLog::getId, logIds)
                        .orderByDesc(EmpMutationLog::getCreateTime));

        return allLogs.stream().map(log -> {
            MutationLogVO vo = new MutationLogVO();
            BeanUtils.copyProperties(log, vo);

            Long recordId = getRecordId(log.getBusinessType(), log.getBusinessId());
            if (recordId != null) {
                List<ApprovalDetail> details = approvalDetailMapper.selectList(
                        new LambdaQueryWrapper<ApprovalDetail>()
                                .eq(ApprovalDetail::getRecordId, recordId)
                                .orderByAsc(ApprovalDetail::getStepOrder));
                vo.setApprovalDetails(details);
            }

            return vo;
        }).collect(Collectors.toList());
    }

    private Long getRecordId(String businessType, Long businessId) {
        if (businessType == null || businessId == null) return null;
        switch (businessType) {
            case "ONBOARDING": {
                HrOnboarding r = hrOnboardingMapper.selectById(businessId);
                return r != null ? r.getRecordId() : null;
            }
            case "REGULARIZATION": {
                HrRegularization r = hrRegularizationMapper.selectById(businessId);
                return r != null ? r.getRecordId() : null;
            }
            case "TRANSFER": {
                HrTransfer r = hrTransferMapper.selectById(businessId);
                return r != null ? r.getRecordId() : null;
            }
            case "RESIGNATION": {
                HrResignation r = hrResignationMapper.selectById(businessId);
                return r != null ? r.getRecordId() : null;
            }
            default:
                return null;
        }
    }
}
