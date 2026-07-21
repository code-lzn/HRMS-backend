package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.ApprovalDelegationMapper;
import com.limou.hrms.model.entity.ApprovalDelegation;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.enums.DelegationStatusEnum;
import com.limou.hrms.model.vo.ApprovalDelegationVO;
import com.limou.hrms.service.ApprovalDelegationService;
import com.limou.hrms.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ApprovalDelegationServiceImpl extends ServiceImpl<ApprovalDelegationMapper, ApprovalDelegation>
        implements ApprovalDelegationService {

    @Resource
    private EmployeeService employeeService;

    @Override
    public ApprovalDelegation createDelegation(Long delegatorEmployeeId, String delegatorName,
                                                Long delegateEmployeeId, String delegateName,
                                                String businessTypes, Date startDate, Date endDate) {
        ThrowUtils.throwIf(delegateEmployeeId == null, ErrorCode.PARAMS_ERROR, "请选择被委托人");
        ThrowUtils.throwIf(startDate == null || endDate == null, ErrorCode.PARAMS_ERROR, "请选择委托日期范围");
        ThrowUtils.throwIf(startDate.after(endDate), ErrorCode.PARAMS_ERROR, "开始日期不能晚于结束日期");
        ThrowUtils.throwIf(delegatorEmployeeId.equals(delegateEmployeeId),
                ErrorCode.PARAMS_ERROR, "不能委托给自己");

        ApprovalDelegation delegation = new ApprovalDelegation();
        delegation.setDelegatorId(delegatorEmployeeId);
        delegation.setDelegatorName(delegatorName);
        delegation.setDelegateId(delegateEmployeeId);
        delegation.setDelegateName(delegateName);
        delegation.setBusinessTypes(businessTypes);
        delegation.setStartDate(startDate);
        delegation.setEndDate(endDate);
        delegation.setStatus(DelegationStatusEnum.ACTIVE.getValue());
        this.save(delegation);
        return delegation;
    }

    @Override
    public void cancelDelegation(Long delegationId, Long delegatorEmployeeId) {
        ApprovalDelegation delegation = this.getById(delegationId);
        ThrowUtils.throwIf(delegation == null, ErrorCode.DELEGATION_NOT_FOUND);
        ThrowUtils.throwIf(!Objects.equals(delegation.getDelegatorId(), delegatorEmployeeId),
                ErrorCode.NO_AUTH_ERROR);
        delegation.setStatus(DelegationStatusEnum.CANCELLED.getValue());
        this.updateById(delegation);
    }

    @Override
    public List<ApprovalDelegationVO> getMyDelegations(Long employeeId) {
        List<ApprovalDelegation> list = this.lambdaQuery()
                .eq(ApprovalDelegation::getDelegatorId, employeeId)
                .eq(ApprovalDelegation::getStatus, DelegationStatusEnum.ACTIVE.getValue())
                .ge(ApprovalDelegation::getEndDate, new Date())
                .orderByDesc(ApprovalDelegation::getCreateTime)
                .list();

        return list.stream().map(d -> {
            ApprovalDelegationVO vo = new ApprovalDelegationVO();
            vo.setId(d.getId());
            vo.setDelegatorName(d.getDelegatorName());
            vo.setDelegateName(d.getDelegateName());
            vo.setBusinessTypes(d.getBusinessTypes());
            vo.setStartDate(d.getStartDate());
            vo.setEndDate(d.getEndDate());
            vo.setStatus(d.getStatus());
            vo.setCreateTime(d.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean isActiveDelegate(Long delegatorId, Long delegateId, String businessType) {
        Date now = new Date();
        List<ApprovalDelegation> delegations = this.lambdaQuery()
                .eq(ApprovalDelegation::getDelegatorId, delegatorId)
                .eq(ApprovalDelegation::getDelegateId, delegateId)
                .eq(ApprovalDelegation::getStatus, DelegationStatusEnum.ACTIVE.getValue())
                .le(ApprovalDelegation::getStartDate, now)
                .ge(ApprovalDelegation::getEndDate, now)
                .list();

        for (ApprovalDelegation d : delegations) {
            if (d.getBusinessTypes() == null || d.getBusinessTypes().isEmpty()) {
                return true; // 全部业务类型
            }
            Set<String> types = new HashSet<>(Arrays.asList(d.getBusinessTypes().split(",")));
            if (types.contains(businessType)) {
                return true;
            }
        }
        return false;
    }
}
