package com.limou.hrms.builder;

import com.limou.hrms.config.ApprovalConfig;
import com.limou.hrms.mapper.OnboardingApplicationMapper;
import com.limou.hrms.mapper.PositionMapper;
import com.limou.hrms.model.entity.ApprovalNode;
import com.limou.hrms.model.entity.OnboardingApplication;
import com.limou.hrms.model.entity.Position;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.enums.NodeStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 入职审批节点构建器：Node1=部门负责人 → Node2=HR负责人（可配置开关 + 自动触发条件）
 * <p>
 * HR二审触发条件（对应PRD 5.1.4）：
 * 1. 配置开关 approval.hr-node.enabled = true
 * 2. 非标准职位：职位未定义职级范围（levelMin/levelMax 为空）
 * 3. 薪资超出职级范围 → 由薪资模块在HR设置新员工薪资时触发额外审批
 */
@Slf4j
@Component
public class OnboardingNodeBuilder implements ApprovalNodeBuilder {

    @Resource
    private OnboardingApplicationMapper onboardingApplicationMapper;
    @Resource
    private PositionMapper positionMapper;
    @Resource
    private ApprovalConfig approvalConfig;
    @Resource
    private ApproverResolver approverResolver;

    @Override
    public List<ApprovalNode> build(Long bizId, Long applicantId) {
        // 1. 查入职申请获取部门ID
        OnboardingApplication app = onboardingApplicationMapper.selectById(bizId);
        if (app == null) {
            throw new IllegalArgumentException("入职申请不存在: " + bizId);
        }

        List<ApprovalNode> nodes = new ArrayList<>();
        int order = 1;

        // Node 1: 部门负责人
        ApprovalNode node1 = new ApprovalNode();
        node1.setNodeName("部门负责人审批");
        node1.setNodeOrder(order++);
        node1.setApproverId(approverResolver.resolveDeptManager(app.getDepartmentId()));
        node1.setStatus(NodeStatus.PENDING.getCode());
        nodes.add(node1);

        // Node 2: HR负责人（可配置开关 + 自动触发条件）
        boolean needHrNode = approvalConfig.getHrNode().isEnabled() || isNonStandardPosition(app.getPositionId());

        if (needHrNode) {
            Long hrApproverId = approverResolver.resolveHrApprover();
            if (hrApproverId != null) {
                ApprovalNode node2 = new ApprovalNode();
                node2.setNodeName("HR负责人审批");
                node2.setNodeOrder(order);
                node2.setApproverId(hrApproverId);
                node2.setStatus(NodeStatus.PENDING.getCode());
                nodes.add(node2);
            }
        }

        return nodes;
    }

    /**
     * 判断是否为非标准职位（未定义职级范围）
     */
    private boolean isNonStandardPosition(Long positionId) {
        if (positionId == null) return false;
        Position position = positionMapper.selectById(positionId);
        boolean nonStandard = position != null
                && (position.getLevelMin() == null || position.getLevelMax() == null);
        if (nonStandard) {
            log.info("非标准职位，自动开启HR二审: positionId={}, name={}", positionId, position.getName());
        }
        return nonStandard;
    }

    @Override
    public ApprovalBizType supportedBizType() {
        return ApprovalBizType.ONBOARDING;
    }
}
