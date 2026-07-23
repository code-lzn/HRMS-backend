package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.ThrowUtils;
import com.limou.hrms.mapper.ApprovalRecordMapper;
import com.limou.hrms.mapper.EmployeeDetailMapper;
import com.limou.hrms.mapper.LeaveMapper;
import com.limou.hrms.mapper.MakeupPunchMapper;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.ApprovalActionEnum;
import com.limou.hrms.model.enums.ApprovalRecordStatusEnum;
import com.limou.hrms.model.enums.ApprovalStatusEnum;
import com.limou.hrms.model.enums.ApproverTypeEnum;
import com.limou.hrms.model.enums.DelegationStatusEnum;
import com.limou.hrms.model.enums.BusinessTypeEnum;
import com.limou.hrms.model.vo.ApprovalApprovedVO;
import com.limou.hrms.model.vo.ApprovalDetailVO;
import com.limou.hrms.model.vo.ApprovalPendingVO;
import com.limou.hrms.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
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
    private EmployeeLeaveBalanceService employeeLeaveBalanceService;

    @Resource
    private ApprovalDelegationService approvalDelegationService;

    @Resource
    private EmployeeService employeeService;

    @Resource
    private DepartmentService departmentService;

    @Resource
    private EmployeeDetailMapper employeeDetailMapper;

    @Resource
    private LeaveMapper leaveMapper;

    @Resource
    private UserService userService;

    @Resource
    private RoleService roleService;

    @Resource
    private MakeupPunchMapper makeupPunchMapper;

    @Resource
    private com.limou.hrms.mapper.OvertimeRecordMapper overtimeRecordMapper;

    @Resource
    @Lazy
    private OnboardingService onboardingService;

    @Resource
    @Lazy
    private RegularizationService regularizationService;

    @Resource
    @Lazy
    private TransferService transferService;

    @Resource
    @Lazy
    private ResignationService resignationService;

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
                .eq(ApprovalDelegation::getStatus, DelegationStatusEnum.ACTIVE.getValue())
                .le(ApprovalDelegation::getStartDate, new Date())
                .ge(ApprovalDelegation::getEndDate, new Date())
                .list();

        //去重--找到委托人id
        Set<Long> delegatorIds = activeDelegations.stream()
                .map(ApprovalDelegation::getDelegatorId)
                .collect(Collectors.toSet());

        // 获取委托人所辖的待审批项
        List<ApprovalDetail> delegatedList = new ArrayList<>();
        if (!delegatorIds.isEmpty()) {
            //委托人id,业务类型去重
            Map<Long, Set<String>> delegatorBusinessTypes = new HashMap<>();
            for (ApprovalDelegation d : activeDelegations) {
                Set<String> types = d.getBusinessTypes() != null
                        ? new HashSet<>(Arrays.asList(d.getBusinessTypes().split(",")))
                        : null;
                delegatorBusinessTypes.put(d.getDelegatorId(), types);
            }
            //拿到对应的需要审批的数据
            List<ApprovalDetail> tempList = approvalDetailService.lambdaQuery()
                    .in(ApprovalDetail::getApproverId, delegatorIds)
                    .eq(ApprovalDetail::getAction, ApprovalActionEnum.PENDING.getValue())
                    .list();
            //加入到对应的list返回
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
            if (!seenRecordIds.add(detail.getRecordId())) continue;

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
    public List<ApprovalApprovedVO> getApprovedList(Long employeeId) {
        List<ApprovalDetail> details = approvalDetailService.lambdaQuery()
                .eq(ApprovalDetail::getApproverId, employeeId)
                .in(ApprovalDetail::getAction,
                        ApprovalActionEnum.APPROVE.getValue(),
                        ApprovalActionEnum.REJECT.getValue(),
                        ApprovalActionEnum.TRANSFER.getValue())
                .orderByDesc(ApprovalDetail::getOperateTime)
                .list();

        Set<Long> seenRecordIds = new HashSet<>();
        List<ApprovalApprovedVO> result = new ArrayList<>();
        for (ApprovalDetail detail : details) {
            if (!seenRecordIds.add(detail.getRecordId())) continue;

            ApprovalRecord record = this.getById(detail.getRecordId());
            if (record == null) continue;

            ApprovalApprovedVO vo = new ApprovalApprovedVO();
            vo.setRecordId(record.getId());
            vo.setDetailId(detail.getId());
            vo.setBusinessType(record.getBusinessType());
            vo.setBusinessTypeText(getBusinessTypeText(record.getBusinessType()));
            vo.setBusinessId(record.getBusinessId());
            vo.setApplicantName(record.getApplicantName());
            vo.setApplyTime(record.getCreateTime());
            vo.setNodeName(detail.getNodeName());
            vo.setAction(detail.getAction());
            vo.setActionText(getActionText(detail.getAction()));
            vo.setOperateTime(detail.getOperateTime());
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

        // 当已存审批明细缺少审批人名称时，尝试动态重新解析（不持久化，仅展示用）
        Employee applicant = record.getApplicantId() != null
                ? employeeService.getById(record.getApplicantId()) : null;

        List<ApprovalDetailVO.NodeDetail> nodes = new ArrayList<>();
        for (ApprovalDetail detail : details) {
            ApprovalDetailVO.NodeDetail node = new ApprovalDetailVO.NodeDetail();
            node.setNodeName(detail.getNodeName());
            node.setStepOrder(detail.getStepOrder());
            String approverName = detail.getApproverName();
            if (approverName == null && detail.getNodeId() != null) {
                ApprovalFlowNode nodeDef = approvalFlowNodeService.getById(detail.getNodeId());
                if (nodeDef != null) {
                    Long resolvedId = resolveApprover(nodeDef, applicant, record.getDepartmentId());
                    if (resolvedId != null) {
                        Employee resolvedEmp = employeeService.getById(resolvedId);
                        if (resolvedEmp != null) {
                            approverName = resolvedEmp.getEmployeeName();
                        }
                    }
                }
            }
            node.setApproverName(approverName);
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

        // 推进审批流程----同步到对应的业务表

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
        // 同步到业务表
        syncBusinessResult(record, false);
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
        return startApproval(businessType, businessId, applicantEmployeeId, applicantName, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApprovalRecord startApproval(String businessType, Long businessId,
                                         Long applicantEmployeeId, String applicantName,
                                         Long targetDepartmentId) {
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
        record.setDepartmentId(targetDepartmentId);
        this.save(record);

        // 创建审批明细节点
        Employee applicant = employeeService.getById(applicantEmployeeId);
        List<ApprovalDetail> details = new ArrayList<>();
        Set<Long> approverIdSet = new HashSet<>();
        for (int i = 0; i < nodes.size(); i++) {
            ApprovalFlowNode node = nodes.get(i);
            Long approverId = resolveApprover(node, applicant, targetDepartmentId);
            if (approverId != null) approverIdSet.add(approverId);

            ApprovalDetail detail = new ApprovalDetail();
            detail.setRecordId(record.getId());
            detail.setNodeId(node.getId());
            detail.setNodeName(node.getNodeName());
            detail.setStepOrder(node.getNodeOrder());
            detail.setApproverId(approverId);
            detail.setAction(ApprovalActionEnum.PENDING.getValue());
            detail.setIsDelegated(0);
            details.add(detail);
        }

        Map<Long, String> nameMap = batchLoadNames(approverIdSet);
        for (ApprovalDetail detail : details) {
            detail.setApproverName(nameMap.get(detail.getApproverId()));
        }
        approvalDetailService.saveBatch(details);

        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApprovalRecord startApproval(String businessType, Long businessId,
                                         Long applicantEmployeeId, String applicantName,
                                         Long targetDepartmentId,
                                         Map<Integer, Long> nodeApproverOverrides) {
        ApprovalFlow flow = approvalFlowService.lambdaQuery()
                .eq(ApprovalFlow::getBusinessType, businessType)
                .eq(ApprovalFlow::getStatus, 1)
                .one();
        ThrowUtils.throwIf(flow == null, ErrorCode.NOT_FOUND_ERROR, "审批流未定义");

        List<ApprovalFlowNode> nodes = approvalFlowNodeService.lambdaQuery()
                .eq(ApprovalFlowNode::getFlowId, flow.getId())
                .orderByAsc(ApprovalFlowNode::getNodeOrder)
                .list();
        ThrowUtils.throwIf(nodes.isEmpty(), ErrorCode.SYSTEM_ERROR, "审批流节点未配置");

        ApprovalRecord record = new ApprovalRecord();
        record.setFlowId(flow.getId());
        record.setBusinessType(businessType);
        record.setBusinessId(businessId);
        record.setApplicantId(applicantEmployeeId);
        record.setApplicantName(applicantName);
        record.setCurrentStep(1);
        record.setTotalSteps(nodes.size());
        record.setStatus(ApprovalRecordStatusEnum.APPROVING.getValue());
        record.setDepartmentId(targetDepartmentId);
        this.save(record);

        Employee applicant = employeeService.getById(applicantEmployeeId);
        List<ApprovalDetail> details = new ArrayList<>();
        Set<Long> approverIdSet = new HashSet<>();
        for (int i = 0; i < nodes.size(); i++) {
            ApprovalFlowNode node = nodes.get(i);
            Long approverId = null;
            if (nodeApproverOverrides != null && nodeApproverOverrides.containsKey(node.getNodeOrder())) {
                approverId = nodeApproverOverrides.get(node.getNodeOrder());
            } else {
                approverId = resolveApprover(node, applicant, targetDepartmentId);
            }
            if (approverId != null) approverIdSet.add(approverId);

            ApprovalDetail detail = new ApprovalDetail();
            detail.setRecordId(record.getId());
            detail.setNodeId(node.getId());
            detail.setNodeName(node.getNodeName());
            detail.setStepOrder(node.getNodeOrder());
            detail.setApproverId(approverId);
            detail.setAction(ApprovalActionEnum.PENDING.getValue());
            detail.setIsDelegated(0);
            details.add(detail);
        }

        Map<Long, String> nameMap = batchLoadNames(approverIdSet);
        for (ApprovalDetail detail : details) {
            detail.setApproverName(nameMap.get(detail.getApproverId()));
        }
        approvalDetailService.saveBatch(details);

        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApprovalRecord startApprovalByFlowId(Long flowId, String businessType, Long businessId,
                                                 Long applicantEmployeeId, String applicantName) {
        ApprovalFlow flow = approvalFlowService.getById(flowId);
        ThrowUtils.throwIf(flow == null || flow.getStatus() == null || flow.getStatus() != 1,
                ErrorCode.NOT_FOUND_ERROR, "审批流未定义或已禁用");

        List<ApprovalFlowNode> nodes = approvalFlowNodeService.lambdaQuery()
                .eq(ApprovalFlowNode::getFlowId, flow.getId())
                .orderByAsc(ApprovalFlowNode::getNodeOrder)
                .list();
        ThrowUtils.throwIf(nodes.isEmpty(), ErrorCode.SYSTEM_ERROR, "审批流节点未配置");

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

        Employee applicant = employeeService.getById(applicantEmployeeId);
        List<ApprovalDetail> details = new ArrayList<>();
        Set<Long> approverIdSet = new HashSet<>();
        for (ApprovalFlowNode node : nodes) {
            Long approverId = resolveApprover(node, applicant);
            if (approverId != null) approverIdSet.add(approverId);

            ApprovalDetail detail = new ApprovalDetail();
            detail.setRecordId(record.getId());
            detail.setNodeId(node.getId());
            detail.setNodeName(node.getNodeName());
            detail.setStepOrder(node.getNodeOrder());
            detail.setApproverId(approverId);
            detail.setAction(ApprovalActionEnum.PENDING.getValue());
            detail.setIsDelegated(0);
            details.add(detail);
        }

        Map<Long, String> nameMap = batchLoadNames(approverIdSet);
        for (ApprovalDetail detail : details) {
            detail.setApproverName(nameMap.get(detail.getApproverId()));
        }
        approvalDetailService.saveBatch(details);

        return record;
    }

    // ========== 私有方法 ==========

    private Map<Long, String> batchLoadNames(Set<Long> employeeIds) {
        if (employeeIds.isEmpty()) return Collections.emptyMap();
        return employeeService.lambdaQuery()
                .in(Employee::getId, employeeIds)
                .list().stream()
                .collect(Collectors.toMap(Employee::getId, Employee::getEmployeeName, (a, b) -> a));
    }

    private ApprovalDetail validateAndGetDetail(Long detailId, Long employeeId) {
        ApprovalDetail detail = approvalDetailService.getById(detailId);
        ThrowUtils.throwIf(detail == null, ErrorCode.APPROVAL_NOT_FOUND);
        ThrowUtils.throwIf(!ApprovalActionEnum.PENDING.getValue().equals(detail.getAction()), ErrorCode.APPROVAL_NOT_PENDING);

        // 检查审批记录是否仍在审批中
        ApprovalRecord record = this.getById(detail.getRecordId());
        ThrowUtils.throwIf(record == null, ErrorCode.APPROVAL_NOT_FOUND);
        ThrowUtils.throwIf(!ApprovalRecordStatusEnum.APPROVING.getValue().equals(record.getStatus()),
                ErrorCode.APPROVAL_NOT_PENDING);

        boolean hasPermission = Objects.equals(detail.getApproverId(), employeeId);
        if (!hasPermission) {
            hasPermission = approvalDelegationService.isActiveDelegate(detail.getApproverId(), employeeId,
                    record.getBusinessType());
        }
        ThrowUtils.throwIf(!hasPermission, ErrorCode.APPROVAL_NO_PERMISSION);

        if (!Objects.equals(detail.getApproverId(), employeeId)) {
            detail.setIsDelegated(1);
            detail.setDelegatedBy(detail.getApproverId());
            detail.setApproverId(employeeId);
        }

        return detail;
    }

    private void advanceApproval(Long recordId) {
        ApprovalRecord record = this.getById(recordId);
        int nextStep = record.getCurrentStep() + 1;
        if (nextStep > record.getTotalSteps()) {
            // 所有节点已通过
            record.setStatus(ApprovalRecordStatusEnum.APPROVED.getValue());
            record.setFinishedAt(new Date());
            this.updateById(record);
            // 同步到业务表
            syncBusinessResult(record, true);
        } else {
            record.setCurrentStep(nextStep);
            this.updateById(record);
        }
    }

    /**
     * 审批结果同步到业务表
     *
     * @param record  审批记录
     * @param isApproved  true=通过, false=拒绝
     */
    private void syncBusinessResult(ApprovalRecord record, boolean isApproved) {
        BusinessTypeEnum bizType = BusinessTypeEnum.getEnumByValue(record.getBusinessType());
        if (bizType == null) return;

        Integer targetStatus = isApproved ? ApprovalStatusEnum.APPROVED.getValue()
                                          : ApprovalStatusEnum.REJECTED.getValue();
        Date now = new Date();

        switch (bizType) {
            case LEAVE: {
                Leave leave = leaveMapper.selectById(record.getBusinessId());
                if (leave != null) {
                    leave.setStatus(targetStatus);
                    leave.setApproveTime(now);
                    leaveMapper.updateById(leave);
                    if (isApproved) {
                        employeeLeaveBalanceService.deduct(
                                leave.getEmployeeId(), leave.getLeaveType(), leave.getTotalDays());
                    } else {
                        // 拒绝后还原假期余额 (如果之前已扣减)
                        employeeLeaveBalanceService.restore(
                                leave.getEmployeeId(), leave.getLeaveType(), leave.getTotalDays());
                    }
                }
                break;
            }
            case PATCH_CLOCK: {
                MakeupPunch punch = makeupPunchMapper.selectById(record.getBusinessId());
                if (punch != null) {
                    punch.setStatus(targetStatus);
                    punch.setApproveTime(now);
                    makeupPunchMapper.updateById(punch);
                }
                break;
            }
            case ONBOARDING: {
                if (isApproved) {
                    onboardingService.onApprovalPassed(record.getBusinessId());
                }
                log.info("业务类型 [{}] 审批完成，businessId={}, status={}",
                        bizType.getText(), record.getBusinessId(), targetStatus);
                break;
            }
            case REGULARIZATION: {
                if (isApproved) {
                    regularizationService.onApprovalPassed(record.getBusinessId());
                } else {
                    regularizationService.onApprovalRejected(record.getBusinessId());
                }
                break;
            }
            case TRANSFER: {
                if (isApproved) {
                    transferService.onApprovalPassed(record.getBusinessId());
                } else {
                    transferService.onApprovalRejected(record.getBusinessId());
                }
                break;
            }
            case RESIGNATION: {
                if (isApproved) {
                    resignationService.onApprovalPassed(record.getBusinessId());
                } else {
                    resignationService.onApprovalRejected(record.getBusinessId());
                }
                break;
            }
            case SALARY_BATCH:
                log.info("业务类型 [{}] 审批完成，businessId={}, status={}",
                        bizType.getText(), record.getBusinessId(), targetStatus);
                break;
            case OVERTIME: {
                OvertimeRecord overtime = overtimeRecordMapper.selectById(record.getBusinessId());
                if (overtime != null) {
                    overtime.setStatus(targetStatus);
                    overtime.setApproveTime(now);
                    overtimeRecordMapper.updateById(overtime);
                }
                break;
            }
        }
    }

    @Override
    public List<ApprovalDetail> getApprovalDetails(Long recordId) {
        return approvalDetailService.lambdaQuery()
                .eq(ApprovalDetail::getRecordId, recordId)
                .orderByAsc(ApprovalDetail::getStepOrder)
                .list();
    }

    /**
     * 解析审批人
     *
     * @param node      节点
     * @param applicant 申请人
     * @return 审批人
     */

    private Long resolveApprover(ApprovalFlowNode node, Employee applicant) {
        return resolveApprover(node, applicant, null);
    }
    //解析审批流；部门id是否为空进行校验-有部门id，部门主管进行审批，
    // 没有的话是直接上级进行审批
    //否则是这个节点的审批人id进行负责

    private Long resolveApprover(ApprovalFlowNode node, Employee applicant, Long targetDepartmentId) {
        //审批类型校验
        ApproverTypeEnum approverType = ApproverTypeEnum.getEnumByValue(node.getApproverType());
        //当审批类型为空时，直接返回节点的审批人
        if (approverType == null) return node.getApproverId();
        //审批类型不是空的时候，根据审批类型进行审批人解析
        switch (approverType) {
            case DEPT_MANAGER: {
                if (applicant == null) return null;
                Long deptId = targetDepartmentId != null ? targetDepartmentId : applicant.getDepartmentId();
                if (deptId != null) {
                    Department dept = departmentService.getById(deptId);
                    if (dept != null && dept.getManagerId() != null) {
                        return dept.getManagerId();
                    }
                }
                return null;
            }
            case DIRECT_SUPERIOR: {
                if (applicant == null) return 15L; // 默认系统管理员limou
                EmployeeDetail detail = employeeDetailMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EmployeeDetail>()
                                .eq(EmployeeDetail::getEmployeeId, applicant.getId())
                );
                Long directReportId = detail != null ? detail.getDirectReportId() : null;
                return directReportId != null ? directReportId : 15L; // 找不到直接汇报人时默认limou
            }
            case HR_MANAGER: {
                if (node.getApproverId() != null) {
                    return node.getApproverId();
                }
                // 动态查找HR角色用户
                List<Role> roles = roleService.lambdaQuery()
                        .like(Role::getRoleCode, "HR")
                        .list();
                if (roles.isEmpty()) {
                    roles = roleService.lambdaQuery()
                            .like(Role::getRoleName, "HR")
                            .list();
                }
                for (Role role : roles) {
                    User hrUser = userService.lambdaQuery()
                            .eq(User::getRoleId, role.getId())
                            .isNotNull(User::getEmployeeId)
                            .last("LIMIT 1")
                            .one();
                    if (hrUser != null && hrUser.getEmployeeId() != null) {
                        return hrUser.getEmployeeId();
                    }
                }
                return null;
            }
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
