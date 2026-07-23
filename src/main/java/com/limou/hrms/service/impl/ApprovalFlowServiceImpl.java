package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.builder.ApprovalNodeBuilder;
import com.limou.hrms.builder.ApprovalNodeBuilderFactory;
import com.limou.hrms.builder.ApproverResolver;
import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.constant.DataScopeContext;
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
    private UserMapper userMapper;
    @Resource
    private ApprovalDelegateService approvalDelegateService;
    @Resource
    private ApproverResolver approverResolver;
    @Resource
    private DataScopeContext dataScopeContext;
    @Resource
    private CacheManager cacheManager;
    @Resource
    private OnboardingApplicationMapper onboardingApplicationMapper;
    @Resource
    private ProbationApplicationMapper probationApplicationMapper;
    @Resource
    private TransferApplicationMapper transferApplicationMapper;
    @Resource
    private ResignationApplicationMapper resignationApplicationMapper;
    @Resource
    private LeaveRequestMapper leaveRequestMapper;
    @Resource
    private SupplementCardRequestMapper supplementCardRequestMapper;
    @Resource
    private SalaryBatchMapper salaryBatchMapper;
    @Resource
    private DepartmentMapper departmentMapper;
    @Resource
    private PositionMapper positionMapper;

    /** Spring 自动注入所有 ApprovalCallback 实现（各业务模块），可能为空。
     *  @Lazy 避免循环依赖：ApprovalFlowServiceImpl → List&lt;ApprovalCallback&gt; → OnboardingServiceImpl → ApprovalFlowService */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    private List<ApprovalCallback> callbacks = new ArrayList<>();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApprovalInstance createInstance(ApprovalBizType bizType, Long bizId, Long applicantId) {
        // 0. 检查是否存在已撤回的实例，有则复用（避免 uk_biz 唯一键冲突）
        ApprovalInstance exist = approvalInstanceMapper.selectOne(
                Wrappers.<ApprovalInstance>lambdaQuery()
                        .eq(ApprovalInstance::getBizType, bizType.getCode())
                        .eq(ApprovalInstance::getBizId, bizId)
                        .last("LIMIT 1"));
        if (exist != null) {
            // 删除旧节点，重置状态为 PENDING
            approvalNodeMapper.delete(
                    Wrappers.<ApprovalNode>lambdaQuery()
                            .eq(ApprovalNode::getInstanceId, exist.getId()));
            exist.setStatus(ApprovalStatus.PENDING.getCode());
            exist.setCurrentNodeOrder(1);
            exist.setCreateTime(LocalDateTime.now());
            approvalInstanceMapper.updateById(exist);
        }

        // 1. 查 Builder 构建节点链
        ApprovalNodeBuilder builder = builderFactory.getBuilder(bizType);
        List<ApprovalNode> nodes = builder.build(bizId, applicantId);

        // 2. 生成审批标题
        String applicantName = approverResolver.getEmployeeName(applicantId);
        String title = applicantName + "的" + bizType.getDesc();

        // 3. 保存审批实例（复用或新建）
        ApprovalInstance instance;
        if (exist != null) {
            instance = exist;
        } else {
            instance = new ApprovalInstance();
            instance.setBizType(bizType.getCode());
            instance.setBizId(bizId);
            instance.setTitle(title);
            instance.setStatus(ApprovalStatus.PENDING.getCode());
            instance.setApplicantId(applicantId);
            instance.setCurrentNodeOrder(1);
            instance.setCreateTime(LocalDateTime.now());
            approvalInstanceMapper.insert(instance);
        }
        instance.setTitle(title); // 标题可能变化

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
    public long getPendingCount() {
        Long employeeId = resolveCurrentEmployeeId();
        // 先从缓存读取
        org.springframework.cache.Cache cache = cacheManager.getCache("pendingCount");
        if (cache != null) {
            org.springframework.cache.Cache.ValueWrapper cached = cache.get(employeeId);
            if (cached != null) {
                return (long) cached.get();
            }
        }
        // 1. 自己的待办
        long myCount = approvalNodeMapper.selectCount(
                new QueryWrapper<ApprovalNode>()
                        .eq("approver_id", employeeId)
                        .eq("status", NodeStatus.PENDING.getCode()));

        // 2. 委托给我的（别人委托给 currentUser，委托有效期内）
        Long delegatedCount = approvalNodeMapper.selectCount(
                new QueryWrapper<ApprovalNode>()
                        .eq("status", NodeStatus.PENDING.getCode())
                        .inSql("approver_id",
                                "SELECT d.delegator_id FROM approval_delegate d " +
                                "WHERE d.delegate_id = " + employeeId + " " +
                                "AND d.enabled = 1 AND NOW() BETWEEN d.start_time AND d.end_time"));

        long count = myCount + delegatedCount;
        // 写入缓存
        if (cache != null) {
            cache.put(employeeId, count);
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long nodeId, String comment) {
        Long employeeId = resolveCurrentEmployeeId();
        evictPendingCountCache(employeeId);
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
    public void reject(Long nodeId, String comment) {
        if (StringUtils.isBlank(comment)) {
            log.warn("拒绝时必须填写审批意见: nodeId={}", nodeId);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "拒绝时必须填写审批意见");
        }
        Long employeeId = resolveCurrentEmployeeId();
        evictPendingCountCache(employeeId);
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
        log.info("审批节点已拒绝: nodeId={}", nodeId);

        // 更新实例
        ApprovalInstance instance = getInstanceOrThrow(node.getInstanceId());
        instance.setStatus(ApprovalStatus.REJECTED.getCode());
        approvalInstanceMapper.updateById(instance);

        // 触发拒绝回调
        invokeCallback(instance, false);
        log.info("审批实例已拒绝: instanceId={}", instance.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transfer(Long nodeId, Long toEmployeeId) {
        Long fromEmployeeId = resolveCurrentEmployeeId();
        evictPendingCountCacheAll();
        ApprovalNode node = getNodeOrThrow(nodeId);
        validateNodeOwner(node, fromEmployeeId);
        validateNodePending(node);

        if (toEmployeeId.equals(fromEmployeeId)) {
            log.warn("不能转交给自己: nodeId={}", nodeId);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能转交给自己");
        }
        // 验证目标审批人存在
        Employee target = employeeMapper.selectById(toEmployeeId);
        if (target == null) {
            log.warn("目标审批人不存在: nodeId={}", nodeId);
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
        node.setTransferred(true);
        // 状态保持待审批
        node.setStatus(NodeStatus.PENDING.getCode());
        node.setComment(null);
        node.setOperateTime(LocalDateTime.now());
        approvalNodeMapper.updateById(node);

        log.info("审批已转交: nodeId={}, from={}, to={}", nodeId, fromEmployeeId, toEmployeeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long instanceId) {
        Long operatorId = resolveCurrentEmployeeId();
        evictPendingCountCache(operatorId);
        ApprovalInstance instance = getInstanceOrThrow(instanceId);

        // 校验：仅申请人可撤回
        if (!instance.getApplicantId().equals(operatorId)) {
            log.warn("仅申请人可撤回 instanceId={}, operatorId={}", instanceId, operatorId);
            throw new BusinessException(ErrorCode.APPROVAL_NODE_NOT_OWNER);
        }
        // 校验：必须是审批中状态
        if (ApprovalStatus.PENDING.getCode() != instance.getStatus()) {
            log.warn("当前状态不允许撤回 instanceId={}", instanceId);
            throw new BusinessException(ErrorCode.APPROVAL_NODE_ALREADY_HANDLED);
        }
        // 校验：必须是第一节点
        if (instance.getCurrentNodeOrder() != 1) {
            log.warn("仅第一节点可撤回 instanceId={}", instanceId);
            throw new BusinessException(ErrorCode.APPROVAL_CANCEL_ONLY_FIRST_NODE);
        }

        instance.setStatus(ApprovalStatus.CANCELLED.getCode());
        approvalInstanceMapper.updateById(instance);

        // 将所有待审批节点标记为已撤回，审批人不再看到该待办
        List<ApprovalNode> pendingNodes = approvalNodeMapper.selectList(
                new QueryWrapper<ApprovalNode>()
                        .eq("instance_id", instanceId)
                        .eq("status", NodeStatus.PENDING.getCode()));
        for (ApprovalNode node : pendingNodes) {
            node.setStatus(NodeStatus.CANCELLED.getCode());
            node.setOperateTime(LocalDateTime.now());
            approvalNodeMapper.updateById(node);
        }

        log.info("审批已撤回: instanceId={}, 已标记{}个节点为已撤回", instanceId, pendingNodes.size());
    }

    @Override
    public Page<PendingItemVO> getPendingList(ApprovalQuery query) {
        Long employeeId = resolveCurrentEmployeeId();
        String role = resolveCurrentUserRole();
        // 角色路由：admin/hr → 全平台，dept_head → 本部门，其他 → 仅自己
        if (isAdminOrHr(role)) {
            return getAllPendingList(query);
        }
        if (isDeptHead(role)) {
            Long deptId = approverResolver.resolveDepartmentId(employeeId);
            return queryDeptPendingList(deptId, query);
        }
        return queryPersonalPendingList(employeeId, query);
    }

    /** 个人待办（含委托路由） */
    private Page<PendingItemVO> queryPersonalPendingList(Long employeeId, ApprovalQuery query) {
        // 排除我委托出去的审批节点
        String excludeDelegatedSql = "NOT EXISTS (SELECT 1 FROM approval_delegate d " +
                "WHERE d.delegator_id = " + employeeId + " " +
                "AND d.enabled = 1 AND NOW() BETWEEN d.start_time AND d.end_time)";

        // 全量取自己的待办节点
        List<ApprovalNode> ownNodes = approvalNodeMapper.selectList(
                new QueryWrapper<ApprovalNode>()
                        .eq("approver_id", employeeId)
                        .eq("status", NodeStatus.PENDING.getCode())
                        .apply(excludeDelegatedSql)
                        .orderByDesc("create_time"));

        // 全量取委托给我的节点
        String delegateInSql = "SELECT d.delegator_id FROM approval_delegate d " +
                "WHERE d.delegate_id = " + employeeId + " " +
                "AND d.enabled = 1 AND NOW() BETWEEN d.start_time AND d.end_time";
        List<ApprovalNode> delegateNodes = approvalNodeMapper.selectList(
                new QueryWrapper<ApprovalNode>()
                        .eq("status", NodeStatus.PENDING.getCode())
                        .inSql("approver_id", delegateInSql)
                        .orderByDesc("create_time"));

        // 记录委托节点ID
        Set<Long> delegateNodeIds = delegateNodes.stream()
                .map(ApprovalNode::getId).collect(Collectors.toSet());

        // 合并、去重、排序
        Set<Long> seen = new HashSet<>();
        List<ApprovalNode> merged = new ArrayList<>();
        for (ApprovalNode n : ownNodes) { seen.add(n.getId()); merged.add(n); }
        for (ApprovalNode n : delegateNodes) {
            if (seen.add(n.getId())) merged.add(n);
        }
        merged.sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));

        // 全部构建 VO，用户自己的节点都可操作
        Set<Long> myActionableIds = merged.stream().map(ApprovalNode::getId).collect(Collectors.toSet());
        List<PendingItemVO> allRecords = buildPendingVOs(merged, delegateNodeIds, myActionableIds);

        // bizType 筛选（先筛选再分页）
        if (StringUtils.isNotBlank(query.getBizType())) {
            allRecords = allRecords.stream()
                    .filter(r -> query.getBizType().equals(r.getBizType()))
                    .collect(Collectors.toList());
        }

        // 内存分页
        int total = allRecords.size();
        int from = Math.min((query.getCurrent() - 1) * query.getPageSize(), total);
        int to = Math.min(from + query.getPageSize(), total);
        List<PendingItemVO> pageRecords = allRecords.subList(from, to);

        Page<PendingItemVO> result = new Page<>(query.getCurrent(), query.getPageSize(), total);
        result.setRecords(pageRecords);
        return result;
    }

    /** 全平台待办（admin/hr） */
    private Page<PendingItemVO> getAllPendingList(ApprovalQuery query) {
        Long employeeId = resolveCurrentEmployeeId();
        // 先取全部 PENDING 节点，整体去重后再分页，避免分页导致的去重不一致
        List<ApprovalNode> allNodes = approvalNodeMapper.selectList(
                new QueryWrapper<ApprovalNode>()
                        .eq("status", NodeStatus.PENDING.getCode())
                        .orderByDesc("create_time"));

        // admin/hr 可操作任意节点
        Set<Long> actionableNodeIds = allNodes.stream()
                .map(ApprovalNode::getId)
                .collect(Collectors.toSet());

        List<PendingItemVO> allRecords = buildPendingVOs(allNodes, new HashSet<>(), actionableNodeIds);
        // 按 instanceId 去重，确保同个审批只出现一次
        Set<Long> seen = new HashSet<>();
        allRecords = allRecords.stream()
                .filter(r -> seen.add(r.getInstanceId()))
                .collect(Collectors.toList());
        log.info("getAllPendingList: allNodes={}, afterDedup={}, bizType={}",
                allNodes.size(), allRecords.size(), query.getBizType());
        if (StringUtils.isNotBlank(query.getBizType())) {
            allRecords = allRecords.stream()
                    .filter(r -> query.getBizType().equals(r.getBizType()))
                    .collect(Collectors.toList());
            log.info("getAllPendingList: afterBizTypeFilter={}, current={}, pageSize={}",
                    allRecords.size(), query.getCurrent(), query.getPageSize());
        }
        // 内存分页
        int total = allRecords.size();
        int from = Math.min((query.getCurrent() - 1) * query.getPageSize(), total);
        int to = Math.min(from + query.getPageSize(), total);
        List<PendingItemVO> pageRecords = allRecords.subList(from, to);

        log.info("getAllPendingList: total={}, page records={}", total, pageRecords.size());
        Page<PendingItemVO> result = new Page<>(query.getCurrent(), query.getPageSize(), total);
        result.setRecords(pageRecords);
        return result;
    }

    /** 判断当前用户是否可以操作该审批节点 */
    private boolean canActOnNode(ApprovalNode node, Long employeeId) {
        if (node.getApproverId().equals(employeeId)) return true;
        try {
            Long resolved = approvalDelegateService.resolveApprover(node.getApproverId());
            return employeeId.equals(resolved);
        } catch (Exception e) {
            return false;
        }
    }

    /** 部门待办 */
    private Page<PendingItemVO> queryDeptPendingList(Long departmentId, ApprovalQuery query) {
        // 全量取本部门审批人对应的待办节点，内存分页，避免 selectPage + inSql 不一致
        List<ApprovalNode> allNodes = approvalNodeMapper.selectList(
                new QueryWrapper<ApprovalNode>()
                        .eq("status", NodeStatus.PENDING.getCode())
                        .inSql("approver_id",
                                "SELECT e.id FROM employee e " +
                                "JOIN employee_work_info w ON w.employee_id = e.id " +
                                "WHERE w.department_id = " + departmentId + " AND e.is_deleted = 0")
                        .orderByDesc("create_time"));

        Set<Long> deptActionableIds = allNodes.stream()
                .map(ApprovalNode::getId).collect(Collectors.toSet());
        List<PendingItemVO> allRecords = buildPendingVOs(allNodes, new HashSet<>(), deptActionableIds);
        if (StringUtils.isNotBlank(query.getBizType())) {
            allRecords = allRecords.stream()
                    .filter(r -> query.getBizType().equals(r.getBizType()))
                    .collect(Collectors.toList());
        }
        // 内存分页
        int total = allRecords.size();
        int from = Math.min((query.getCurrent() - 1) * query.getPageSize(), total);
        int to = Math.min(from + query.getPageSize(), total);
        List<PendingItemVO> pageRecords = allRecords.subList(from, to);

        Page<PendingItemVO> result = new Page<>(query.getCurrent(), query.getPageSize(), total);
        result.setRecords(pageRecords);
        return result;
    }

    @Override
    public Page<ProcessedItemVO> getProcessedList(ApprovalQuery query) {
        Long employeeId = resolveCurrentEmployeeId();
        String role = resolveCurrentUserRole();
        // 角色路由：admin/hr → 全平台，dept_head → 本部门，其他 → 仅自己
        if (isAdminOrHr(role)) {
            return getAllProcessedList(query);
        }
        if (isDeptHead(role)) {
            Long deptId = approverResolver.resolveDepartmentId(employeeId);
            return queryDeptProcessedList(deptId, query);
        }
        return queryPersonalProcessedList(employeeId, query);
    }

    /** 个人已办 */
    private Page<ProcessedItemVO> queryPersonalProcessedList(Long employeeId, ApprovalQuery query) {
        List<ApprovalNode> allNodes = approvalNodeMapper.selectList(
                new QueryWrapper<ApprovalNode>()
                        .eq("approver_id", employeeId)
                        .in("status", NodeStatus.APPROVED.getCode(), NodeStatus.REJECTED.getCode(),
                                NodeStatus.TRANSFERRED.getCode(), NodeStatus.TIMEOUT.getCode())
                        .orderByDesc("operate_time"));

        List<ProcessedItemVO> allRecords = buildProcessedVOs(allNodes);
        if (StringUtils.isNotBlank(query.getBizType())) {
            allRecords = allRecords.stream()
                    .filter(r -> query.getBizType().equals(r.getBizType()))
                    .collect(Collectors.toList());
        }
        int total = allRecords.size();
        int from = Math.min((query.getCurrent() - 1) * query.getPageSize(), total);
        int to = Math.min(from + query.getPageSize(), total);
        Page<ProcessedItemVO> result = new Page<>(query.getCurrent(), query.getPageSize(), total);
        result.setRecords(allRecords.subList(from, to));
        return result;
    }

    /** 全平台已办 */
    private Page<ProcessedItemVO> getAllProcessedList(ApprovalQuery query) {
        List<ApprovalNode> allNodes = approvalNodeMapper.selectList(
                new QueryWrapper<ApprovalNode>()
                        .in("status", NodeStatus.APPROVED.getCode(), NodeStatus.REJECTED.getCode(),
                                NodeStatus.TRANSFERRED.getCode(), NodeStatus.TIMEOUT.getCode())
                        .orderByDesc("operate_time"));

        List<ProcessedItemVO> allRecords = buildProcessedVOs(allNodes);
        if (StringUtils.isNotBlank(query.getBizType())) {
            allRecords = allRecords.stream()
                    .filter(r -> query.getBizType().equals(r.getBizType()))
                    .collect(Collectors.toList());
        }
        int total = allRecords.size();
        int from = Math.min((query.getCurrent() - 1) * query.getPageSize(), total);
        int to = Math.min(from + query.getPageSize(), total);
        Page<ProcessedItemVO> result = new Page<>(query.getCurrent(), query.getPageSize(), total);
        result.setRecords(allRecords.subList(from, to));
        return result;
    }

    /** 部门已办 */
    private Page<ProcessedItemVO> queryDeptProcessedList(Long departmentId, ApprovalQuery query) {
        List<ApprovalNode> allNodes = approvalNodeMapper.selectList(
                new QueryWrapper<ApprovalNode>()
                        .in("status", NodeStatus.APPROVED.getCode(), NodeStatus.REJECTED.getCode(),
                                NodeStatus.TRANSFERRED.getCode(), NodeStatus.TIMEOUT.getCode())
                        .inSql("approver_id",
                                "SELECT e.id FROM employee e " +
                                "JOIN employee_work_info w ON w.employee_id = e.id " +
                                "WHERE w.department_id = " + departmentId + " AND e.is_deleted = 0")
                        .orderByDesc("operate_time"));

        List<ProcessedItemVO> allRecords = buildProcessedVOs(allNodes);
        if (StringUtils.isNotBlank(query.getBizType())) {
            allRecords = allRecords.stream()
                    .filter(r -> query.getBizType().equals(r.getBizType()))
                    .collect(Collectors.toList());
        }
        int total = allRecords.size();
        int from = Math.min((query.getCurrent() - 1) * query.getPageSize(), total);
        int to = Math.min(from + query.getPageSize(), total);
        Page<ProcessedItemVO> result = new Page<>(query.getCurrent(), query.getPageSize(), total);
        result.setRecords(allRecords.subList(from, to));
        return result;
    }

    // ==================== 角色判定 ====================

    private boolean isAdminOrHr(String role) {
        return "admin".equals(role) || "hr".equals(role);
    }

    private boolean isDeptHead(String role) {
        return "dept_head".equals(role);
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
        if (instance.getCreateTime() != null) {
            vo.setDeadLine(instance.getCreateTime().plusHours(48));
        }

        List<ApprovalNodeVO> nodeVOs = nodes.stream().map(node -> {
            ApprovalNodeVO nvo = new ApprovalNodeVO();
            nvo.setNodeId(node.getId());
            nvo.setNodeName(node.getNodeName());
            nvo.setNodeOrder(node.getNodeOrder());
            nvo.setApproverId(node.getApproverId());
            nvo.setApproverName(approverResolver.getEmployeeName(node.getApproverId()));
            boolean isTransfer = Boolean.TRUE.equals(node.getTransferred());
            // 查委托关系：如果审批人有活跃委托，显示原审批人→被委托人
            Long resolvedId = approvalDelegateService.resolveApprover(node.getApproverId());
            if (resolvedId != null && !resolvedId.equals(node.getApproverId())) {
                nvo.setOriginalApproverId(node.getApproverId());
                nvo.setOriginalApproverName(approverResolver.getEmployeeName(node.getApproverId()));
                nvo.setApproverId(resolvedId);
                nvo.setApproverName(approverResolver.getEmployeeName(resolvedId));
                nvo.setTransferred(isTransfer);
            } else {
                nvo.setOriginalApproverId(node.getOriginalApproverId());
                nvo.setOriginalApproverName(node.getOriginalApproverId() != null
                        ? approverResolver.getEmployeeName(node.getOriginalApproverId()) : null);
                nvo.setTransferred(isTransfer);
            }
            nvo.setStatus(node.getStatus());
            NodeStatus ns = NodeStatus.fromCode(node.getStatus());
            nvo.setStatusDesc(ns != null ? ns.getDesc() : "");
            nvo.setComment(node.getComment());
            nvo.setOperateTime(node.getOperateTime());
            return nvo;
        }).collect(Collectors.toList());
        vo.setNodes(nodeVOs);

        // 填充业务申请表数据，前端据此渲染"申请详情"区域
        Map<String, Object> bizData = queryBizData(instance);
        vo.setBizData(bizData);
        log.info("审批详情查询完成: instanceId={}, bizType={}, bizId={}, bizDataKeys={}",
                instanceId, instance.getBizType(), instance.getBizId(), bizData.keySet());

        return vo;
    }

    /**
     * 根据审批实例的业务类型和业务 ID，查询对应的申请表数据并转为 Map，
     * 同时补充显示名（如部门名、职位名、性别描述等）。
     */
    private Map<String, Object> queryBizData(ApprovalInstance instance) {
        ApprovalBizType bizType = ApprovalBizType.fromCode(instance.getBizType());
        Long bizId = instance.getBizId();
        if (bizType == null || bizId == null) return Collections.emptyMap();

        try {
            switch (bizType) {
                case ONBOARDING:
                    return enrichOnboarding(onboardingApplicationMapper.selectById(bizId));
                case PROBATION:
                    return enrichProbation(probationApplicationMapper.selectById(bizId));
                case TRANSFER:
                    return enrichTransfer(transferApplicationMapper.selectById(bizId));
                case RESIGNATION:
                    return enrichResignation(resignationApplicationMapper.selectById(bizId));
                case LEAVE:
                    return enrichLeave(leaveRequestMapper.selectById(bizId));
                case CARD_REPLENISH:
                    return enrichSupplementCard(supplementCardRequestMapper.selectById(bizId));
                case SALARY_BATCH:
                    return enrichSalaryBatch(salaryBatchMapper.selectById(bizId));
                default:
                    return Collections.emptyMap();
            }
        } catch (Exception e) {
            log.warn("查询业务申请表数据失败: bizType={}, bizId={}", instance.getBizType(), bizId, e);
            return Collections.emptyMap();
        }
    }

    /** 入职申请：实体只存 ID，需要补显示名 */
    private Map<String, Object> enrichOnboarding(OnboardingApplication app) {
        if (app == null) return Collections.emptyMap();
        Map<String, Object> map = beanToMap(app);
        // 性别
        if (app.getGender() != null) {
            map.put("genderDesc", app.getGender() == 1 ? "男" : "女");
        }
        // 部门名
        if (app.getDepartmentId() != null) {
            Department dept = departmentMapper.selectById(app.getDepartmentId());
            if (dept != null) map.put("departmentName", dept.getName());
        }
        // 职位名
        if (app.getPositionId() != null) {
            Position pos = positionMapper.selectById(app.getPositionId());
            if (pos != null) map.put("positionName", pos.getName());
        }
        // 录用类型
        if (app.getHireType() != null) {
            String[] hireTypes = {"", "全职", "兼职", "实习"};
            map.put("hireTypeDesc", app.getHireType() > 0 && app.getHireType() < hireTypes.length ? hireTypes[app.getHireType()] : "");
        }
        // 直接汇报人姓名
        if (app.getDirectReportId() != null) {
            map.put("directReportName", approverResolver.getEmployeeName(app.getDirectReportId()));
        }
        // 试用期月数（前端用 probationsMonths，实体是 defaultProbationMonths）
        map.put("probationMonths", app.getDefaultProbationMonths());
        return map;
    }

    /** 转正申请：补员工姓名 */
    private Map<String, Object> enrichProbation(ProbationApplication app) {
        if (app == null) return Collections.emptyMap();
        Map<String, Object> map = beanToMap(app);
        if (app.getEmployeeId() != null) {
            map.put("employeeName", approverResolver.getEmployeeName(app.getEmployeeId()));
        }
        return map;
    }

    /** 调岗申请：补员工名、部门名、职位名 */
    private Map<String, Object> enrichTransfer(TransferApplication app) {
        if (app == null) return Collections.emptyMap();
        Map<String, Object> map = beanToMap(app);
        if (app.getEmployeeId() != null) {
            map.put("employeeName", approverResolver.getEmployeeName(app.getEmployeeId()));
        }
        if (app.getFromDepartmentId() != null) {
            Department d = departmentMapper.selectById(app.getFromDepartmentId());
            if (d != null) map.put("fromDepartmentName", d.getName());
        }
        if (app.getToDepartmentId() != null) {
            Department d = departmentMapper.selectById(app.getToDepartmentId());
            if (d != null) map.put("toDepartmentName", d.getName());
        }
        if (app.getFromPositionId() != null) {
            Position p = positionMapper.selectById(app.getFromPositionId());
            if (p != null) map.put("fromPositionName", p.getName());
        }
        if (app.getToPositionId() != null) {
            Position p = positionMapper.selectById(app.getToPositionId());
            if (p != null) map.put("toPositionName", p.getName());
        }
        return map;
    }

    /** 离职申请：补员工名、交接人名、离职类型描述 */
    private Map<String, Object> enrichResignation(ResignationApplication app) {
        if (app == null) return Collections.emptyMap();
        Map<String, Object> map = beanToMap(app);
        if (app.getEmployeeId() != null) {
            map.put("employeeName", approverResolver.getEmployeeName(app.getEmployeeId()));
        }
        if (app.getHandoverToId() != null) {
            map.put("handoverToName", approverResolver.getEmployeeName(app.getHandoverToId()));
        }
        if (app.getResignationType() != null) {
            String[] types = {"", "辞职", "辞退", "合同到期不续签", "其他"};
            map.put("resignationTypeDesc", app.getResignationType() > 0 && app.getResignationType() < types.length ? types[app.getResignationType()] : "");
        }
        return map;
    }

    /** 请假申请：补员工姓名、请假类型描述、时间格式化 */
    private Map<String, Object> enrichLeave(LeaveRequest app) {
        if (app == null) return Collections.emptyMap();
        Map<String, Object> map = beanToMap(app);
        if (app.getEmployeeId() != null) {
            map.put("employeeName", approverResolver.getEmployeeName(app.getEmployeeId()));
        }
        // 请假类型描述
        if (app.getLeaveType() != null) {
            String[] types = {"", "年假", "病假", "事假", "婚假", "产假", "丧假", "调休"};
            map.put("leaveTypeDesc", app.getLeaveType() > 0 && app.getLeaveType() < types.length ? types[app.getLeaveType()] : "");
        }
        // 时间格式化为空格分隔（替代 ISO 的 T）
        if (app.getStartTime() != null) {
            map.put("startTime", app.getStartTime().toString().replace("T", " "));
        }
        if (app.getEndTime() != null) {
            map.put("endTime", app.getEndTime().toString().replace("T", " "));
        }
        return map;
    }

    /** 补卡申请：补员工姓名、卡类型描述 */
    private Map<String, Object> enrichSupplementCard(SupplementCardRequest app) {
        if (app == null) return Collections.emptyMap();
        Map<String, Object> map = beanToMap(app);
        if (app.getEmployeeId() != null) {
            map.put("employeeName", approverResolver.getEmployeeName(app.getEmployeeId()));
        }
        map.put("cardTypeDesc", app.getCardType() == 1 ? "上班卡" : "下班卡");
        return map;
    }

    /** 薪资批次：补批次号、月份、人数、金额 */
    private Map<String, Object> enrichSalaryBatch(SalaryBatch batch) {
        if (batch == null) return Collections.emptyMap();
        Map<String, Object> map = beanToMap(batch);
        return map;
    }

    /** 将实体对象转为 Map，直接用 BeanUtils 描述符遍历属性 */
    private Map<String, Object> beanToMap(Object obj) {
        if (obj == null) return Collections.emptyMap();
        Map<String, Object> map = new LinkedHashMap<>();
        try {
            java.beans.BeanInfo info = java.beans.Introspector.getBeanInfo(obj.getClass());
            for (java.beans.PropertyDescriptor pd : info.getPropertyDescriptors()) {
                String name = pd.getName();
                if ("class".equals(name)) continue;
                try {
                    Object value = pd.getReadMethod().invoke(obj);
                    if (value != null) {
                        map.put(name, value);
                    }
                } catch (Exception ignored) {
                    // skip unreadable properties
                }
            }
        } catch (Exception e) {
            log.warn("beanToMap 转换失败: {}", obj.getClass().getSimpleName(), e);
        }
        return map;
    }

    // ==================== 私有辅助方法 ====================

    private ApprovalNode getNodeOrThrow(Long nodeId) {
        ApprovalNode node = approvalNodeMapper.selectById(nodeId);
        if (node == null) {
            log.warn("审批节点不存在: nodeId={}", nodeId);
            throw new BusinessException(ErrorCode.APPROVAL_NODE_NOT_FOUND);
        }
        return node;
    }

    private ApprovalInstance getInstanceOrThrow(Long instanceId) {
        ApprovalInstance instance = approvalInstanceMapper.selectById(instanceId);
        if (instance == null) {
            log.warn("审批实例不存在: instanceId={}", instanceId);
            throw new BusinessException(ErrorCode.APPROVAL_INSTANCE_NOT_FOUND);
        }
        return instance;
    }

    // ==================== 当前用户解析 ====================

    /** 测试用：直接注入当前用户，绕过 RequestContextHolder */
    private Long testEmployeeId;
    /** 测试用 */
    private String testRole;

    /** 测试用：直接注入当前用户，绕过 RequestContextHolder */
    public void setCurrentUserForTest(Long employeeId, String role) {
        this.testEmployeeId = employeeId;
        this.testRole = role;
    }

    private Long resolveCurrentEmployeeId() {
        if (testEmployeeId != null) return testEmployeeId;
        Long employeeId = dataScopeContext.getCurrentEmployeeId();
        if (employeeId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录或未关联员工档案");
        }
        return employeeId;
    }

    private String resolveCurrentUserRole() {
        if (testRole != null) return testRole;
        String role = dataScopeContext.getCurrentRole();
        return role != null ? role : "user";
    }

    private void evictPendingCountCache(Long employeeId) {
        org.springframework.cache.Cache cache = cacheManager.getCache("pendingCount");
        if (cache != null) {
            cache.evict(employeeId);
        }
    }

    private void evictPendingCountCacheAll() {
        org.springframework.cache.Cache cache = cacheManager.getCache("pendingCount");
        if (cache != null) {
            cache.clear();
        }
    }

    // ==================== 权限校验 ====================

    private void validateNodeOwner(ApprovalNode node, Long employeeId) {
        if (!node.getApproverId().equals(employeeId)) {
            // admin 角色可操作任何节点
            if ("admin".equals(resolveCurrentUserRole())) return;
            // 检查当前用户是否为 node 审批人的有效委托人
            Long resolvedApprover = approvalDelegateService.resolveApprover(node.getApproverId());
            if (!employeeId.equals(resolvedApprover)) {
                log.warn("该节点不属于当前用户: nodeId={}, nodeApproverId={}, currentEmployeeId={}, resolvedDelegate={}, nodeName={}",
                        node.getId(), node.getApproverId(), employeeId, resolvedApprover, node.getNodeName());
                throw new BusinessException(ErrorCode.APPROVAL_NODE_NOT_OWNER);
            }
        }
    }

    private void validateNodePending(ApprovalNode node) {
        if (NodeStatus.TIMEOUT.getCode() == node.getStatus()) {
            log.warn("审批节点超时: nodeId={}", node.getId());
            throw new BusinessException(ErrorCode.APPROVAL_NODE_TIMEOUT);
        }
        if (NodeStatus.PENDING.getCode() != node.getStatus()) {
            log.warn("审批节点已处理, 不能重复操作: nodeId={}", node.getId());
            throw new BusinessException(ErrorCode.APPROVAL_NODE_ALREADY_HANDLED);
        }
    }

    /**
     * 将审批节点列表组装为待办 VO
     */
    private List<PendingItemVO> buildPendingVOs(List<ApprovalNode> nodes, Set<Long> delegateNodeIds, Set<Long> actionableNodeIds) {
        // 批量加载审批实例，避免 N+1
        Set<Long> instanceIds = nodes.stream().map(ApprovalNode::getInstanceId).collect(Collectors.toSet());
        Map<Long, ApprovalInstance> instanceMap = instanceIds.isEmpty() ? Collections.emptyMap() :
                approvalInstanceMapper.selectBatchIds(instanceIds).stream()
                        .collect(Collectors.toMap(ApprovalInstance::getId, i -> i));
        // 批量加载人名
        Set<Long> empIds = new HashSet<>();
        instanceMap.values().forEach(i -> { if (i.getApplicantId() != null) empIds.add(i.getApplicantId()); });
        nodes.stream().filter(n -> delegateNodeIds.contains(n.getId())).forEach(n -> empIds.add(n.getApproverId()));
        Map<Long, String> nameMap = loadEmployeeNames(empIds);
        return nodes.stream()
                .filter(node -> {
                    ApprovalInstance instance = instanceMap.get(node.getInstanceId());
                    return instance != null
                        && node.getNodeOrder().equals(instance.getCurrentNodeOrder())
                        && (instance.getStatus() == null || !instance.getStatus().equals(ApprovalStatus.CANCELLED.getCode()));
                })
                .map(node -> {
            ApprovalInstance instance = instanceMap.get(node.getInstanceId());
            PendingItemVO vo = new PendingItemVO();
            vo.setInstanceId(node.getInstanceId());
            vo.setNodeId(node.getId());
            if (instance != null) {
                vo.setBizType(instance.getBizType());
                ApprovalBizType bizType = ApprovalBizType.fromCode(instance.getBizType());
                vo.setBizTypeDesc(bizType != null ? bizType.getDesc() : instance.getBizType());
                vo.setTitle(instance.getTitle());
                vo.setApplicantId(instance.getApplicantId());
                vo.setApplicantName(nameMap.getOrDefault(instance.getApplicantId(), ""));
                vo.setCreateTime(instance.getCreateTime());
            }
            vo.setNodeName(node.getNodeName());
            vo.setNodeOrder(node.getNodeOrder());
            if (instance.getCreateTime() != null) {
                vo.setDeadLine(instance.getCreateTime().plusHours(48));
            }
            if (delegateNodeIds.contains(node.getId())) {
                vo.setDelegatorName(nameMap.getOrDefault(node.getApproverId(), ""));
            }
            vo.setCanAct(actionableNodeIds != null && actionableNodeIds.contains(node.getId()));
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 将审批节点列表组装为已办 VO
     */
    private List<ProcessedItemVO> buildProcessedVOs(List<ApprovalNode> nodes) {
        Set<Long> instanceIds = nodes.stream().map(ApprovalNode::getInstanceId).collect(Collectors.toSet());
        Map<Long, ApprovalInstance> instanceMap = instanceIds.isEmpty() ? Collections.emptyMap() :
                approvalInstanceMapper.selectBatchIds(instanceIds).stream()
                        .collect(Collectors.toMap(ApprovalInstance::getId, i -> i));
        Set<Long> empIds = new HashSet<>();
        instanceMap.values().forEach(i -> { if (i.getApplicantId() != null) empIds.add(i.getApplicantId()); });
        Map<Long, String> nameMap = loadEmployeeNames(empIds);
        return nodes.stream().map(node -> {
            ApprovalInstance instance = instanceMap.get(node.getInstanceId());
            ProcessedItemVO vo = new ProcessedItemVO();
            vo.setInstanceId(node.getInstanceId());
            vo.setNodeId(node.getId());
            if (instance != null) {
                vo.setBizType(instance.getBizType());
                ApprovalBizType bizType = ApprovalBizType.fromCode(instance.getBizType());
                vo.setBizTypeDesc(bizType != null ? bizType.getDesc() : instance.getBizType());
                vo.setTitle(instance.getTitle());
                vo.setApplicantName(nameMap.getOrDefault(instance.getApplicantId(), ""));
            }
            vo.setNodeName(node.getNodeName());
            vo.setNodeStatus(node.getStatus());
            NodeStatus ns = NodeStatus.fromCode(node.getStatus());
            vo.setNodeStatusDesc(ns != null ? ns.getDesc() : "");
            vo.setComment(node.getComment());
            vo.setOperateTime(node.getOperateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    private Map<Long, String> loadEmployeeNames(Set<Long> empIds) {
        if (empIds.isEmpty()) return Collections.emptyMap();
        Set<Long> validIds = new HashSet<>(empIds);
        validIds.remove(null);
        if (validIds.isEmpty()) return Collections.emptyMap();
        List<Employee> emps = employeeMapper.selectBatchIds(validIds);
        Set<Long> userIds = emps.stream().map(Employee::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> userNameMap = userIds.isEmpty() ? Collections.emptyMap() :
                userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getUserName, (a, b) -> a));
        return emps.stream()
                .collect(Collectors.toMap(
                    Employee::getId,
                    e -> e.getUserId() != null ? userNameMap.getOrDefault(e.getUserId(), e.getEmployeeNo()) : e.getEmployeeNo(),
                    (a, b) -> a));
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
