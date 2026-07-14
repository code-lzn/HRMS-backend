package com.limou.hrms.builder;

import com.limou.hrms.config.ApprovalConfig;
import com.limou.hrms.mapper.DepartmentMapper;
import com.limou.hrms.mapper.TransferApplicationMapper;
import com.limou.hrms.model.entity.ApprovalNode;
import com.limou.hrms.model.entity.Department;
import com.limou.hrms.model.entity.TransferApplication;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.enums.NodeStatus;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 调岗审批节点构建器：Node1=原部门负责人 → Node2=新部门负责人 → Node3=HR负责人（可配置开关）
 */
@Component
public class TransferNodeBuilder implements ApprovalNodeBuilder {

    @Resource
    private TransferApplicationMapper transferApplicationMapper;
    @Resource
    private DepartmentMapper departmentMapper;
    @Resource
    private ApprovalConfig approvalConfig;
    @Resource
    private ApproverResolver approverResolver;

    @Override
    public List<ApprovalNode> build(Long bizId, Long applicantId) {
        TransferApplication app = transferApplicationMapper.selectById(bizId);
        if (app == null) {
            throw new IllegalArgumentException("调岗申请不存在: " + bizId);
        }

        Department fromDept = departmentMapper.selectById(app.getFromDepartmentId());
        if (fromDept == null || fromDept.getManagerId() == null) {
            throw new IllegalArgumentException("原部门或部门负责人不存在");
        }

        Department toDept = departmentMapper.selectById(app.getToDepartmentId());
        if (toDept == null || toDept.getManagerId() == null) {
            throw new IllegalArgumentException("新部门或部门负责人不存在");
        }

        List<ApprovalNode> nodes = new ArrayList<>();
        int order = 1;

        // Node 1: 原部门负责人
        ApprovalNode node1 = new ApprovalNode();
        node1.setNodeName("原部门负责人审批");
        node1.setNodeOrder(order++);
        node1.setApproverId(fromDept.getManagerId());
        node1.setStatus(NodeStatus.PENDING.getCode());
        nodes.add(node1);

        // Node 2: 新部门负责人
        ApprovalNode node2 = new ApprovalNode();
        node2.setNodeName("新部门负责人审批");
        node2.setNodeOrder(order++);
        node2.setApproverId(toDept.getManagerId());
        node2.setStatus(NodeStatus.PENDING.getCode());
        nodes.add(node2);

        // Node 3: HR负责人（可配置开关，默认开启）
        if (approvalConfig.getHrNode().isEnabled()) {
            Long hrApproverId = approverResolver.resolveHrApprover();
            if (hrApproverId != null) {
                ApprovalNode node3 = new ApprovalNode();
                node3.setNodeName("HR负责人审批");
                node3.setNodeOrder(order);
                node3.setApproverId(hrApproverId);
                node3.setStatus(NodeStatus.PENDING.getCode());
                nodes.add(node3);
            }
        }

        return nodes;
    }

    @Override
    public ApprovalBizType supportedBizType() {
        return ApprovalBizType.TRANSFER;
    }
}
