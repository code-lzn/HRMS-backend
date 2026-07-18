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
import java.util.ArrayList;
import java.util.List;
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
    private EmployeeService employeeService;

    @Override
    public List<MutationLogVO> getMyMutationLogs(Long userId) {
        Employee emp = employeeService.getByUserId(userId);
        if (emp == null) {
            return new ArrayList<>();
        }

        List<EmpMutationLog> logs = empMutationLogMapper.selectList(
                new LambdaQueryWrapper<EmpMutationLog>()
                        .eq(EmpMutationLog::getEmployeeId, emp.getId())
                        .orderByDesc(EmpMutationLog::getCreateTime));

        return logs.stream().map(log -> {
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
