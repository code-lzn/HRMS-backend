package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.ApprovalRecordMapper;
import com.limou.hrms.mapper.EmployeeDetailMapper;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.ApprovalActionEnum;
import com.limou.hrms.model.enums.ApprovalRecordStatusEnum;
import com.limou.hrms.model.enums.ApproverTypeEnum;
import com.limou.hrms.model.enums.BusinessTypeEnum;
import com.limou.hrms.model.vo.ApprovalDetailVO;
import com.limou.hrms.model.vo.ApprovalPendingVO;
import com.limou.hrms.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ApprovalServiceImpl extends ServiceImpl<ApprovalRecordMapper, ApprovalRecord>
        implements ApprovalService {

    @Resource
    private ApprovalFlowService approvalFlowService;

    @Resource
    private ApprovalFlowNodeService approvalFlowNodeService;

    @Resource
    private ApprovalDetailService approvalDetailService;

    @Resource
    private ApprovalDelegationService approvalDelegationService;

    @Resource
    private EmployeeService employeeService;

    @Resource
    private DepartmentService departmentService;

    @Resource
    private EmployeeDetailMapper employeeDetailMapper;

    @Override
    public List<ApprovalPendingVO> getPendingList(Long employeeId) {
        // 1. 直接审批：当前人是审批人且状态为 PENDING
        List<ApprovalDetail> directList = approvalDetailService.lambdaQuery()
                .eq(ApprovalDetail::getApproverId, employeeId)
                .eq(ApprovalDetail::getAction, ApprovalActionEnum.PENDING.getValue())
                .list();

        // 2. 委托审批：当前人是被委托人，且委托在有效期内
        List<ApprovalDelegation> activeDelegations = approvalDelegationService.lambdaQuery()
                .eq(ApprovalDelegation::getDelegateId, employeeId)
                .eq(ApprovalDelegation::getStatus, 1)
                .le(ApprovalDelegation::getStartDate, new Date())
                .ge(ApprovalDelegation::getEndDate, new Date())
                .list();

        Set<Long> delegatorIds = activeDelegations.stream()
                .map(ApprovalDelegation::getDelegatorId)
                .collect(Collectors.toSet());

        // 获取委托人所辖的待审批项
        List<ApprovalDetail> delegatedList = new ArrayList<>();
        if (!delegatorIds.isEmpty()) {
            // 收集委托的业务类型范围
            Map<Long, Set<String>> delegatorBusinessTypes = new HashMap<>();
            for (ApprovalDelegation d : activeDelegations) {
                Set<String> types = d.getBusinessTypes() != null
                        ? new HashSet<>(Arrays.asList(d.getBusinessTypes().split(",")))
                        : null; // null = 全部业务类型
                delegatorBusinessTypes.put(d.getDelegatorId(), types);
            }

            List<ApprovalDetail> tempList = approvalDetailService.lambdaQuery()
                    .in(ApprovalDetail::getApproverId, delegatorIds)
                    .eq(ApprovalDetail::getAction, ApprovalActionEnum.PENDING.getValue())
                    .list();

            for (ApprovalDetail detail : tempList) {
                ApprovalRecord record = this.getById(detail.getRecordId());
                if (record == null) continue;
                Set<String> allowedTypes = delegatorBusinessTypes.get(detail.getApproverId());
                if (allowedTypes == null || allowedTypes.contains(record.getBusinessType())) {
                    delegatedList.add(detail);
                }
            }
        }

        // 合并结果
        Set<Long> seenRecordIds = new HashSet<>();
        List<ApprovalDetail> allDetails = new ArrayList<>();
        allDetails.addAll(directList);
        allDetails.addAll(delegatedList);

        List<ApprovalPendingVO> result = new ArrayList<>();
        for (ApprovalDetail detail : allDetails) {
            if (!seenRecordIds.add(detail.getRecordId())) continue; // 去重

            ApprovalRecord record = this.getById(detail.getRecordId());
            if (record == null || !ApprovalRecordStatusEnum.APPROVING.getValue().equals(record.getStatus())) continue;

            ApprovalPendingVO vo = new ApprovalPendingVO();
            vo.setRecordId(record.getId());
            vo.setDetailId(detail.getId());
            vo.setBusinessType(record.getBusinessType());
            vo.setBusinessTypeText(getBusinessTypeText(record.getBusinessType()));
            vo.setBusinessId(record.getBusinessId());
            vo.setApplicantName(record.getApplicantName());
            vo.setApplyTime(record.getCreateTime());
            vo.setCurrentNodeName(detail.getNodeName());
            result.add(vo);
        }
        return result;
    }

    @Override
    public ApprovalDetailVO getApprovalDetail(Long recordId) {
        ApprovalRecord record = this.getById(recordId);
        ThrowUtils.throwIf(record == null, ErrorCode.APPROVAL_NOT_FOUND);

        ApprovalDetailVO vo = new ApprovalDetailVO();
        vo.setRecordId(record.getId());
        vo.setBusinessType(record.getBusinessType());
        vo.setBusinessTypeText(getBusinessTypeText(record.getBusinessType()));
        vo.setBusinessId(record.getBusinessId());
        vo.setApplicantName(record.getApplicantName());
        vo.setStatus(record.getStatus());
        vo.setStatusText(getStatusText(record.getStatus()));
        vo.setCurrentStep(record.getCurrentStep());
        vo.setTotalSteps(record.getTotalSteps());
        vo.setApplyTime(record.getCreateTime());
        vo.setFinishedAt(record.getFinishedAt());

        // 审批节点历史
        List<ApprovalDetail> details = approvalDetailService.lambdaQuery()
                .eq(ApprovalDetail::getRecordId, recordId)
                .orderByAsc(ApprovalDetail::getStepOrder)
                .list();

        List<ApprovalDetailVO.NodeDetail> nodes = new ArrayList<>();
        for (ApprovalDetail detail : details) {
            ApprovalDetailVO.NodeDetail node = new ApprovalDetailVO.NodeDetail();
            node.setNodeName(detail.getNodeName());
            node.setStepOrder(detail.getStepOrder());
            node.setApproverName(detail.getApproverName());
            node.setAction(detail.getAction());
            node.setActionText(getActionText(detail.getAction()));
            node.setComment(detail.getComment());
            node.setIsDelegated(detail.getIsDelegated());
            if (detail.getIsDelegated() != null && detail.getIsDelegated() == 1 && detail.getDelegatedBy() != null) {
                Employee delegator = employeeService.getById(detail.getDelegatedBy());
                node.setDelegatedByName(delegator != null ? delegator.getEmployeeName() : null);
            }
            node.setOperateTime(detail.getOperateTime());
            nodes.add(node);
        }
        vo.setNodeHistory(nodes);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long detailId, Long employeeId, String comment) {
        ApprovalDetail detail = validateAndGetDetail(detailId, employeeId);

        detail.setAction(ApprovalActionEnum.APPROVE.getValue());
        detail.setComment(comment);
        detail.setOperateTime(new Date());
        approvalDetailService.updateById(detail);

        // 推进审批流程
        advanceApproval(detail.getRecordId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long detailId, Long employeeId, String comment) {
        ApprovalDetail detail = validateAndGetDetail(detailId, employeeId);

        detail.setAction(ApprovalActionEnum.REJECT.getValue());
        detail.setComment(comment);
        detail.setOperateTime(new Date());
        approvalDetailService.updateById(detail);

        // 整个审批流标记为已拒绝
        ApprovalRecord record = this.getById(detail.getRecordId());
        record.setStatus(ApprovalRecordStatusEnum.REJECTED.getValue());
        record.setFinishedAt(new Date());
        this.updateById(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transfer(Long detailId, Long employeeId, Long targetEmployeeId, String comment) {
        ApprovalDetail detail = validateAndGetDetail(detailId, employeeId);

        // 原审批人标记为转交
        detail.setAction(ApprovalActionEnum.TRANSFER.getValue());
        detail.setComment(comment);
        detail.setOperateTime(new Date());
        approvalDetailService.updateById(detail);

        // 创建新的审批明细给目标审批人
        Employee targetEmp = employeeService.getById(targetEmployeeId);
        ThrowUtils.throwIf(targetEmp == null, ErrorCode.NOT_FOUND_ERROR, "目标审批人不存在");

        ApprovalDetail newDetail = new ApprovalDetail();
        newDetail.setRecordId(detail.getRecordId());
        newDetail.setNodeId(detail.getNodeId());
        newDetail.setNodeName(detail.getNodeName());
        newDetail.setStepOrder(detail.getStepOrder());
        newDetail.setApproverId(targetEmployeeId);
        newDetail.setApproverName(targetEmp.getEmployeeName());
        newDetail.setAction(ApprovalActionEnum.PENDING.getValue());
        newDetail.setIsDelegated(0);
        approvalDetailService.save(newDetail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApprovalRecord startApproval(String businessType, Long businessId,
                                         Long applicantEmployeeId, String applicantName) {
        // 查找审批流定义
        ApprovalFlow flow = approvalFlowService.lambdaQuery()
                .eq(ApprovalFlow::getBusinessType, businessType)
                .eq(ApprovalFlow::getStatus, 1)
                .one();
        ThrowUtils.throwIf(flow == null, ErrorCode.NOT_FOUND_ERROR, "审批流未定义");

        // 获取节点列表
        List<ApprovalFlowNode> nodes = approvalFlowNodeService.lambdaQuery()
                .eq(ApprovalFlowNode::getFlowId, flow.getId())
                .orderByAsc(ApprovalFlowNode::getNodeOrder)
                .list();
        ThrowUtils.throwIf(nodes.isEmpty(), ErrorCode.SYSTEM_ERROR, "审批流节点未配置");

        // 创建审批实例
        ApprovalRecord record = new ApprovalRecord();
        record.setFlowId(flow.getId());
        record.setBusinessType(businessType);
        record.setBusinessId(businessId);
        record.setApplicantId(applicantEmployeeId);
        record.setApplicantName(applicantName);
        record.setCurrentStep(1);
        record.setTotalSteps(nodes.size());
        record.setStatus(ApprovalRecordStatusEnum.APPROVING.getValue());
        this.save(record);

        // 创建审批明细节点
        Employee applicant = employeeService.getById(applicantEmployeeId);
        for (int i = 0; i < nodes.size(); i++) {
            ApprovalFlowNode node = nodes.get(i);
            Long approverId = resolveApprover(node, applicant);
            String approverName = null;
            if (approverId != null) {
                Employee approver = employeeService.getById(approverId);
                approverName = approver != null ? approver.getEmployeeName() : null;
            }

            ApprovalDetail detail = new ApprovalDetail();
            detail.setRecordId(record.getId());
            detail.setNodeId(node.getId());
            detail.setNodeName(node.getNodeName());
            detail.setStepOrder(node.getNodeOrder());
            detail.setApproverId(approverId);
            detail.setApproverName(approverName);
            detail.setAction(ApprovalActionEnum.PENDING.getValue()); // 只有第一个节点是 PENDING，后续节点等前面的通过后才变为 PENDING
            // 实际上所有节点都先创建为 PENDING，前端按 stepOrder 过滤即可
            // 但为了精确，可以让第一个节点 PENDING，其余保持为 PENDING 也行
            detail.setIsDelegated(0);
            approvalDetailService.save(detail);
        }

        return record;
    }

    // ========== 私有方法 ==========

    private ApprovalDetail validateAndGetDetail(Long detailId, Long employeeId) {
        ApprovalDetail detail = approvalDetailService.getById(detailId);
        ThrowUtils.throwIf(detail == null, ErrorCode.APPROVAL_NOT_FOUND);
        ThrowUtils.throwIf(!ApprovalActionEnum.PENDING.getValue().equals(detail.getAction()), ErrorCode.APPROVAL_NOT_PENDING);

        // 检查权限：是审批人本人，或者是有效被委托人
        boolean hasPermission = Objects.equals(detail.getApproverId(), employeeId);
        if (!hasPermission) {
            hasPermission = approvalDelegationService.isActiveDelegate(detail.getApproverId(), employeeId,
                    this.getById(detail.getRecordId()).getBusinessType());
        }
        ThrowUtils.throwIf(!hasPermission, ErrorCode.APPROVAL_NO_PERMISSION);

        // 如果是被委托人，标记代审批
        if (!Objects.equals(detail.getApproverId(), employeeId)) {
            detail.setIsDelegated(1);
            detail.setDelegatedBy(detail.getApproverId());
            detail.setApproverId(employeeId);
        }

        return detail;
    }

    private void advanceApproval(Long recordId) {
        ApprovalRecord record = this.getById(recordId);
        List<ApprovalDetail> details = approvalDetailService.lambdaQuery()
                .eq(ApprovalDetail::getRecordId, recordId)
                .orderByAsc(ApprovalDetail::getStepOrder)
                .list();

        int nextStep = record.getCurrentStep() + 1;
        if (nextStep > record.getTotalSteps()) {
            // 所有节点已通过
            record.setStatus(ApprovalRecordStatusEnum.APPROVED.getValue());
            record.setFinishedAt(new Date());
        } else {
            record.setCurrentStep(nextStep);
        }
        this.updateById(record);
    }
    /**
     * 解析审批人
     *
     * @param node      节点
     * @param applicant 申请人
     * @return 审批人
     */

    private Long resolveApprover(ApprovalFlowNode node, Employee applicant) {
        if (applicant == null) return null;

        ApproverTypeEnum approverType = ApproverTypeEnum.getEnumByValue(node.getApproverType());
        if (approverType == null) return node.getApproverId();

        switch (approverType) {
            case DEPT_MANAGER:
                if (applicant.getDepartmentId() != null) {
                    Department dept = departmentService.getById(applicant.getDepartmentId());
                    if (dept != null && dept.getManagerId() != null) {
                        return dept.getManagerId();
                    }
                }
                return null;
            case DIRECT_SUPERIOR: {
                EmployeeDetail detail = employeeDetailMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EmployeeDetail>()
                                .eq(EmployeeDetail::getEmployeeId, applicant.getId())
                );
                return detail != null ? detail.getDirectReportId() : null;
            }
            case HR_MANAGER:
            case FINANCE:
            case BOSS:
            case SPECIFIED:
                return node.getApproverId();
            default:
                return node.getApproverId();
        }
    }

    // ========== 文本映射 ==========

    private String getBusinessTypeText(String businessType) {
        BusinessTypeEnum enumVal = BusinessTypeEnum.getEnumByValue(businessType);
        return enumVal != null ? enumVal.getText() : (businessType != null ? businessType : "未知");
    }

    private String getStatusText(String status) {
        ApprovalRecordStatusEnum enumVal = ApprovalRecordStatusEnum.getEnumByValue(status);
        return enumVal != null ? enumVal.getText() : (status != null ? status : "未知");
    }

    private String getActionText(String action) {
        ApprovalActionEnum enumVal = ApprovalActionEnum.getEnumByValue(action);
        return enumVal != null ? enumVal.getText() : (action != null ? action : "未知");
    }
}
