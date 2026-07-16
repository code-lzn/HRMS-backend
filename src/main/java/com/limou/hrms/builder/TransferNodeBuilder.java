package com.limou.hrms.builder;

import com.limou.hrms.mapper.TransferApplicationMapper;
import com.limou.hrms.model.entity.ApprovalNode;
import com.limou.hrms.model.entity.TransferApplication;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.enums.NodeStatus;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 调岗审批节点构建器：Node1=原部门负责人 → Node2=新部门负责人 → Node3=HR负责人（必须）
 */
@Component
public class TransferNodeBuilder implements ApprovalNodeBuilder {

    @Resource
    private TransferApplicationMapper transferApplicationMapper;
    @Resource
    private ApproverResolver approverResolver;

    @Override
    public List<ApprovalNode> build(Long bizId, Long applicantId) {
        // 1. 查调岗申请获取原/新部门ID
        TransferApplication app = transferApplicationMapper.selectById(bizId);
        if (app == null) {
            throw new IllegalArgumentException("调岗申请不存在: " + bizId);
        }

        List<ApprovalNode> nodes = new ArrayList<>();
        int order = 1;

        // Node 1: 原部门负责人
        ApprovalNode node1 = new ApprovalNode();
        node1.setNodeName("原部门负责人审批");
        node1.setNodeOrder(order++);
        node1.setApproverId(approverResolver.resolveDeptManager(app.getFromDepartmentId()));
        node1.setStatus(NodeStatus.PENDING.getCode());
        nodes.add(node1);

        // Node 2: 新部门负责人
        ApprovalNode node2 = new ApprovalNode();
        node2.setNodeName("新部门负责人审批");
        node2.setNodeOrder(order++);
        node2.setApproverId(approverResolver.resolveDeptManager(app.getToDepartmentId()));
        node2.setStatus(NodeStatus.PENDING.getCode());
        nodes.add(node2);

        // Node 3: HR负责人（必须）
        Long hrApproverId = approverResolver.resolveHrApprover();
        if (hrApproverId != null) {
            ApprovalNode node3 = new ApprovalNode();
            node3.setNodeName("HR负责人审批");
            node3.setNodeOrder(order);
            node3.setApproverId(hrApproverId);
            node3.setStatus(NodeStatus.PENDING.getCode());
            nodes.add(node3);
        }

        return nodes;
    }

    @Override
    public ApprovalBizType supportedBizType() {
        return ApprovalBizType.TRANSFER;
    }
}
