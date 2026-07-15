package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.builder.ApprovalNodeBuilder;
import com.limou.hrms.builder.ApprovalNodeBuilderFactory;
import com.limou.hrms.builder.ApproverResolver;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.BusinessException;
import com.limou.hrms.mapper.*;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.enums.ApprovalStatus;
import com.limou.hrms.model.enums.NodeStatus;
import com.limou.hrms.model.query.ApprovalQuery;
import com.limou.hrms.model.vo.*;
import com.limou.hrms.service.ApprovalCallback;
import com.limou.hrms.service.ApprovalDelegateService;
import com.limou.hrms.service.ApprovalFlowService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ApprovalFlowServiceImpl extends ServiceImpl<ApprovalInstanceMapper, ApprovalInstance>
        implements ApprovalFlowService {

    @Resource
    private ApprovalInstanceMapper approvalInstanceMapper;
    @Resource
    private ApprovalNodeMapper approvalNodeMapper;
    @Resource
    private ApprovalNodeBuilderFactory builderFactory;
    @Resource
    private EmployeeMapper employeeMapper;
    @Resource
    private ApprovalDelegateService approvalDelegateService;
    @Resource
    private ApproverResolver approverResolver;
    @Resource
    private CacheManager cacheManager;

    /** Spring 自动注入所有 ApprovalCallback 实现（各业务模块），可能为空 */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private List<ApprovalCallback> callbacks = new ArrayList<>();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApprovalInstance createInstance(ApprovalBizType bizType, Long bizId, Long applicantId) {
        // 1. 查 Builder 构建节点链
        ApprovalNodeBuilder builder = builderFactory.getBuilder(bizType);
        List<ApprovalNode> nodes = builder.build(bizId, applicantId);

        // 2. 生成审批标题
        String applicantName = approverResolver.getEmployeeName(applicantId);
        String title = applicantName + "的" + bizType.getDesc();

        // 3. 保存审批实例
        ApprovalInstance instance = new ApprovalInstance();
        instance.setBizType(bizType.getCode());
        instance.setBizId(bizId);
        instance.setTitle(title);
        instance.setStatus(ApprovalStatus.PENDING.getCode());
        instance.setApplicantId(applicantId);
        instance.setCurrentNodeOrder(1);
        approvalInstanceMapper.insert(instance);

        // 4. 批量保存节点，绑定 instanceId
        for (int i = 0; i < nodes.size(); i++) {
            ApprovalNode node = nodes.get(i);
            node.setInstanceId(instance.getId());
            node.setNodeOrder(i + 1);
            approvalNodeMapper.insert(node);
        }

        log.info("审批实例创建成功: instanceId={}, bizType={}, bizId={}", instance.getId(), bizType.getCode(), bizId);

        // 驱逐第一个审批人的待办缓存（其待办数量 +1）
        if (!nodes.isEmpty()) {
            Long firstApproverId = nodes.get(0).getApproverId();
            if (firstApproverId != null) {
                org.springframework.cache.Cache cache = cacheManager.getCache("pendingCount");
                if (cache != null) {
                    cache.evict(firstApproverId);
                }
            }
        }

        return instance;
    }

    @Override
    @Cacheable(value = "pendingCount", key = "#employeeId")
    public long getPendingCount(Long employeeId) {
        // 1. 自己的待办
        long myCount = approvalNodeMapper.selectCount(
                new QueryWrapper<ApprovalNode>()
                        .eq("approver_id", employeeId)
                        .eq("status", NodeStatus.PENDING.getCode()));

        // 2. 委托给我的（别人委托给 currentUser，委托有效期内）
        // SELECT COUNT(*) FROM approval_node n
        // WHERE n.status = 1 AND n.approver_id IN (
        //   SELECT d.delegator_id FROM approval_delegate d
        //   WHERE d.delegate_id = ? AND d.enabled = 1
        //   AND NOW() BETWEEN d.start_time AND d.end_time
        // )
        Long delegatedCount = approvalNodeMapper.selectCount(
                new QueryWrapper<ApprovalNode>()
                        .eq("status", NodeStatus.PENDING.getCode())
                        .inSql("approver_id",
                                "SELECT d.delegator_id FROM approval_delegate d " +
                                "WHERE d.delegate_id = " + employeeId + " " +
                                "AND d.enabled = 1 AND NOW() BETWEEN d.start_time AND d.end_time"));

        return myCount + delegatedCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "pendingCount", key = "#employeeId")
    public void approve(Long nodeId, Long employeeId, String comment) {
        ApprovalNode node = getNodeOrThrow(nodeId);
        validateNodeOwner(node, employeeId);
        validateNodePending(node);

        // 若当前用户是受委托人（approver_id != employeeId），记录原始审批人
        if (!node.getApproverId().equals(employeeId) && node.getOriginalApproverId() == null) {
            node.setOriginalApproverId(node.getApproverId());
        }

        // 更新当前节点
        node.setApproverId(employeeId);
        node.setStatus(NodeStatus.APPROVED.getCode());
        node.setComment(comment);
        node.setOperateTime(LocalDateTime.now());
        approvalNodeMapper.updateById(node);

        ApprovalInstance instance = getInstanceOrThrow(node.getInstanceId());

        // 判断是否为最后一个节点
        Long totalNodes = approvalNodeMapper.selectCount(
                new QueryWrapper<ApprovalNode>().eq("instance_id", instance.getId())
        );

        if (node.getNodeOrder() >= totalNodes) {
            // 最后节点 — 审批完成
            instance.setStatus(ApprovalStatus.APPROVED.getCode());
            approvalInstanceMapper.updateById(instance);
            // 触发业务回调
            invokeCallback(instance, true);
            log.info("审批全部通过: instanceId={}", instance.getId());
        } else {
            // 推进到下一节点
            instance.setCurrentNodeOrder(node.getNodeOrder() + 1);
            approvalInstanceMapper.updateById(instance);
            log.info("审批通过，流转到下一节点: instanceId={}, nextOrder={}",
                    instance.getId(), instance.getCurrentNodeOrder());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "pendingCount", key = "#employeeId")
    public void reject(Long nodeId, Long employeeId, String comment) {
        if (StringUtils.isBlank(comment)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "拒绝时必须填写审批意见");
        }
        ApprovalNode node = getNodeOrThrow(nodeId);
        validateNodeOwner(node, employeeId);
        validateNodePending(node);

        // 若当前用户是受委托人，记录原始审批人
        if (!node.getApproverId().equals(employeeId) && node.getOriginalApproverId() == null) {
            node.setOriginalApproverId(node.getApproverId());
        }

        // 更新节点
        node.setApproverId(employeeId);
        node.setStatus(NodeStatus.REJECTED.getCode());
        node.setComment(comment);
        node.setOperateTime(LocalDateTime.now());
        approvalNodeMapper.updateById(node);

        // 更新实例
        ApprovalInstance instance = getInstanceOrThrow(node.getInstanceId());
        instance.setStatus(ApprovalStatus.REJECTED.getCode());
        approvalInstanceMapper.updateById(instance);

        // 触发拒绝回调
        invokeCallback(instance, false);
        log.info("审批已拒绝: instanceId={}", instance.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "pendingCount", allEntries = true)
    public void transfer(Long nodeId, Long fromEmployeeId, Long toEmployeeId) {
        ApprovalNode node = getNodeOrThrow(nodeId);
        validateNodeOwner(node, fromEmployeeId);
        validateNodePending(node);

        if (toEmployeeId.equals(fromEmployeeId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能转交给自己");
        }
        // 验证目标审批人存在
        Employee target = employeeMapper.selectById(toEmployeeId);
        if (target == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目标审批人不存在");
        }

        // 若当前用户是受委托人，记录原始审批人
        if (!node.getApproverId().equals(fromEmployeeId) && node.getOriginalApproverId() == null) {
            node.setOriginalApproverId(node.getApproverId());
        }

        // 记录原审批人（当前操作人）
        if (node.getOriginalApproverId() == null) {
            node.setOriginalApproverId(fromEmployeeId);
        }
        // 更换审批人
        node.setApproverId(toEmployeeId);
        // 状态保持待审批
        node.setStatus(NodeStatus.PENDING.getCode());
        node.setComment(null);
        node.setOperateTime(LocalDateTime.now());
        approvalNodeMapper.updateById(node);

        log.info("审批已转交: nodeId={}, from={}, to={}", nodeId, fromEmployeeId, toEmployeeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "pendingCount", key = "#operatorId")
    public void cancel(Long instanceId, Long operatorId) {
        ApprovalInstance instance = getInstanceOrThrow(instanceId);

        // 校验：仅申请人可撤回
        if (!instance.getApplicantId().equals(operatorId)) {
            throw new BusinessException(ErrorCode.APPROVAL_CANCEL_ONLY_FIRST_NODE);
        }
        // 校验：必须是审批中状态
        if (ApprovalStatus.PENDING.getCode() != instance.getStatus()) {
            throw new BusinessException(ErrorCode.APPROVAL_NODE_ALREADY_HANDLED, "当前状态不允许撤回");
        }
        // 校验：必须是第一节点
        if (instance.getCurrentNodeOrder() != 1) {
            throw new BusinessException(ErrorCode.APPROVAL_CANCEL_ONLY_FIRST_NODE);
        }

        instance.setStatus(ApprovalStatus.CANCELLED.getCode());
        approvalInstanceMapper.updateById(instance);

        log.info("审批已撤回: instanceId={}", instanceId);
    }

    @Override
    public Page<PendingItemVO> getPendingList(Long employeeId, ApprovalQuery query) {
        // 1. 查当前用户待审批的节点（自己的）
        QueryWrapper<ApprovalNode> nodeQw = new QueryWrapper<>();
        nodeQw.eq("approver_id", employeeId)
               .eq("status", NodeStatus.PENDING.getCode())
               .orderByDesc("create_time");

        Page<ApprovalNode> nodePage = approvalNodeMapper.selectPage(
                new Page<>(query.getCurrent(), query.getPageSize()), nodeQw
        );

        // 2. 查委托给我的节点（别人委托给我，在有效期内）
        QueryWrapper<ApprovalNode> delegateQw = new QueryWrapper<>();
        delegateQw.eq("status", NodeStatus.PENDING.getCode())
                  .inSql("approver_id",
                          "SELECT d.delegator_id FROM approval_delegate d " +
                          "WHERE d.delegate_id = " + employeeId + " " +
                          "AND d.enabled = 1 AND NOW() BETWEEN d.start_time AND d.end_time")
                  .orderByDesc("create_time");

        java.util.List<ApprovalNode> delegateNodes = approvalNodeMapper.selectList(delegateQw);

        // 记录委托节点ID，用于组装VO时标记 delegatorName
        Set<Long> delegateNodeIds = new HashSet<>();
        for (ApprovalNode n : delegateNodes) {
            delegateNodeIds.add(n.getId());
        }

        // 3. 合并两个来源（去重），按 create_time 排序
        Set<Long> seenIds = new HashSet<>();
        List<ApprovalNode> allNodes = new ArrayList<>();
        for (ApprovalNode node : nodePage.getRecords()) {
            seenIds.add(node.getId());
            allNodes.add(node);
        }
        for (ApprovalNode node : delegateNodes) {
            if (!seenIds.contains(node.getId())) {
                allNodes.add(node);
            }
        }
        allNodes.sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));

        // 4. 分页处理（内存分页）
        int total = allNodes.size();
        int fromIndex = (int) ((query.getCurrent() - 1) * query.getPageSize());
        int toIndex = Math.min(fromIndex + (int) query.getPageSize(), total);
        if (fromIndex >= total) {
            allNodes = new ArrayList<>();
        } else {
            allNodes = allNodes.subList(fromIndex, toIndex);
        }

        // 5. 关联 instance 信息组装 VO
        List<PendingItemVO> records = allNodes.stream().map(node -> {
            ApprovalInstance instance = approvalInstanceMapper.selectById(node.getInstanceId());
            PendingItemVO vo = new PendingItemVO();
            vo.setInstanceId(node.getInstanceId());
            vo.setNodeId(node.getId());
            if (instance != null) {
                vo.setBizType(instance.getBizType());
                ApprovalBizType bizType = ApprovalBizType.fromCode(instance.getBizType());
                vo.setBizTypeDesc(bizType != null ? bizType.getDesc() : instance.getBizType());
                vo.setTitle(instance.getTitle());
                vo.setApplicantId(instance.getApplicantId());
                vo.setApplicantName(approverResolver.getEmployeeName(instance.getApplicantId()));
                vo.setCreateTime(instance.getCreateTime());
            }
            vo.setNodeName(node.getNodeName());
            vo.setNodeOrder(node.getNodeOrder());
            // 截止时间：节点创建后 48h
            if (node.getCreateTime() != null) {
                vo.setDeadLine(node.getCreateTime().plusHours(48));
            }
            // 委托待办：标记委托人姓名
            if (delegateNodeIds.contains(node.getId())) {
                vo.setDelegatorName(approverResolver.getEmployeeName(node.getApproverId()));
            }
            return vo;
        }).collect(Collectors.toList());

        // 按 bizType 筛选
        if (StringUtils.isNotBlank(query.getBizType())) {
            records = records.stream()
                    .filter(r -> query.getBizType().equals(r.getBizType()))
                    .collect(Collectors.toList());
        }

        Page<PendingItemVO> result = new Page<>(query.getCurrent(), query.getPageSize(), total);
        result.setRecords(records);
        return result;
    }

    @Override
    public Page<ProcessedItemVO> getProcessedList(Long employeeId, ApprovalQuery query) {
        QueryWrapper<ApprovalNode> nodeQw = new QueryWrapper<>();
        nodeQw.eq("approver_id", employeeId)
               .in("status", NodeStatus.APPROVED.getCode(), NodeStatus.REJECTED.getCode(), NodeStatus.TRANSFERRED.getCode(), NodeStatus.TIMEOUT.getCode())
               .orderByDesc("operate_time");

        Page<ApprovalNode> nodePage = approvalNodeMapper.selectPage(
                new Page<>(query.getCurrent(), query.getPageSize()), nodeQw
        );

        List<ProcessedItemVO> records = nodePage.getRecords().stream().map(node -> {
            ApprovalInstance instance = approvalInstanceMapper.selectById(node.getInstanceId());
            ProcessedItemVO vo = new ProcessedItemVO();
            vo.setInstanceId(node.getInstanceId());
            vo.setNodeId(node.getId());
            if (instance != null) {
                vo.setBizType(instance.getBizType());
                ApprovalBizType bizType = ApprovalBizType.fromCode(instance.getBizType());
                vo.setBizTypeDesc(bizType != null ? bizType.getDesc() : instance.getBizType());
                vo.setTitle(instance.getTitle());
                vo.setApplicantName(approverResolver.getEmployeeName(instance.getApplicantId()));
            }
            vo.setNodeName(node.getNodeName());
            vo.setNodeStatus(node.getStatus());
            NodeStatus ns = NodeStatus.fromCode(node.getStatus());
            vo.setNodeStatusDesc(ns != null ? ns.getDesc() : "");
            vo.setComment(node.getComment());
            vo.setOperateTime(node.getOperateTime());
            return vo;
        }).collect(Collectors.toList());

        if (StringUtils.isNotBlank(query.getBizType())) {
            records = records.stream()
                    .filter(r -> query.getBizType().equals(r.getBizType()))
                    .collect(Collectors.toList());
        }

        Page<ProcessedItemVO> result = new Page<>(nodePage.getCurrent(), nodePage.getSize(), nodePage.getTotal());
        result.setRecords(records);
        return result;
    }

    @Override
    public ApprovalInstanceVO getDetail(Long instanceId) {
        ApprovalInstance instance = getInstanceOrThrow(instanceId);
        List<ApprovalNode> nodes = approvalNodeMapper.selectList(
                new QueryWrapper<ApprovalNode>()
                        .eq("instance_id", instanceId)
                        .orderByAsc("node_order")
        );

        ApprovalInstanceVO vo = new ApprovalInstanceVO();
        vo.setInstanceId(instance.getId());
        vo.setBizType(instance.getBizType());
        ApprovalBizType bizType = ApprovalBizType.fromCode(instance.getBizType());
        vo.setBizTypeDesc(bizType != null ? bizType.getDesc() : instance.getBizType());
        vo.setTitle(instance.getTitle());
        vo.setStatus(instance.getStatus());
        ApprovalStatus as = ApprovalStatus.fromCode(instance.getStatus());
        vo.setStatusDesc(as != null ? as.getDesc() : "");
        vo.setApplicantId(instance.getApplicantId());
        vo.setApplicantName(approverResolver.getEmployeeName(instance.getApplicantId()));
        vo.setCurrentNodeOrder(instance.getCurrentNodeOrder());
        vo.setCreateTime(instance.getCreateTime());

        List<ApprovalNodeVO> nodeVOs = nodes.stream().map(node -> {
            ApprovalNodeVO nvo = new ApprovalNodeVO();
            nvo.setNodeId(node.getId());
            nvo.setNodeName(node.getNodeName());
            nvo.setNodeOrder(node.getNodeOrder());
            nvo.setApproverId(node.getApproverId());
            nvo.setApproverName(approverResolver.getEmployeeName(node.getApproverId()));
            nvo.setOriginalApproverId(node.getOriginalApproverId());
            nvo.setOriginalApproverName(node.getOriginalApproverId() != null
                    ? approverResolver.getEmployeeName(node.getOriginalApproverId()) : null);
            nvo.setStatus(node.getStatus());
            NodeStatus ns = NodeStatus.fromCode(node.getStatus());
            nvo.setStatusDesc(ns != null ? ns.getDesc() : "");
            nvo.setComment(node.getComment());
            nvo.setOperateTime(node.getOperateTime());
            return nvo;
        }).collect(Collectors.toList());
        vo.setNodes(nodeVOs);

        return vo;
    }

    // ==================== 私有辅助方法 ====================

    private ApprovalNode getNodeOrThrow(Long nodeId) {
        ApprovalNode node = approvalNodeMapper.selectById(nodeId);
        if (node == null) {
            throw new BusinessException(ErrorCode.APPROVAL_NODE_NOT_FOUND);
        }
        return node;
    }

    private ApprovalInstance getInstanceOrThrow(Long instanceId) {
        ApprovalInstance instance = approvalInstanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new BusinessException(ErrorCode.APPROVAL_INSTANCE_NOT_FOUND);
        }
        return instance;
    }

    private void validateNodeOwner(ApprovalNode node, Long employeeId) {
        if (!node.getApproverId().equals(employeeId)) {
            // 检查当前用户是否为 node 审批人的有效委托人
            Long resolvedApprover = approvalDelegateService.resolveApprover(node.getApproverId());
            if (!employeeId.equals(resolvedApprover)) {
                throw new BusinessException(ErrorCode.APPROVAL_NODE_NOT_OWNER);
            }
        }
    }

    private void validateNodePending(ApprovalNode node) {
        if (NodeStatus.TIMEOUT.getCode() == node.getStatus()) {
            throw new BusinessException(ErrorCode.APPROVAL_NODE_TIMEOUT);
        }
        if (NodeStatus.PENDING.getCode() != node.getStatus()) {
            throw new BusinessException(ErrorCode.APPROVAL_NODE_ALREADY_HANDLED);
        }
    }

    /**
     * 触发业务回调（通过/拒绝）
     */
    private void invokeCallback(ApprovalInstance instance, boolean approved) {
        ApprovalBizType bizType = ApprovalBizType.fromCode(instance.getBizType());
        if (bizType == null) {
            log.warn("未知业务类型: {}", instance.getBizType());
            return;
        }
        for (ApprovalCallback callback : callbacks) {
            if (approved) {
                callback.onApproved(bizType, instance.getBizId());
            } else {
                callback.onRejected(bizType, instance.getBizId());
            }
        }
    }
}
