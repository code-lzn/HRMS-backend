package com.limou.hrms.builder;

import com.limou.hrms.mapper.ResignationApplicationMapper;
import com.limou.hrms.model.entity.ApprovalNode;
import com.limou.hrms.model.entity.ResignationApplication;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.enums.NodeStatus;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 离职审批节点构建器：Node1=部门负责人 → Node2=HR负责人（必须）
 */
@Component
public class ResignationNodeBuilder implements ApprovalNodeBuilder {

    @Resource
    private ResignationApplicationMapper resignationApplicationMapper;
    @Resource
    private ApproverResolver approverResolver;

    @Override
    public List<ApprovalNode> build(Long bizId, Long applicantId) {
        ResignationApplication app = resignationApplicationMapper.selectById(bizId);
        if (app == null) {
            throw new IllegalArgumentException("离职申请不存在: " + bizId);
        }

        // 查员工工作信息获取部门ID → 查部门负责人
        Long deptId = approverResolver.resolveDepartmentId(app.getEmployeeId());

        List<ApprovalNode> nodes = new ArrayList<>();
        int order = 1;

        // Node 1: 部门负责人
        ApprovalNode node1 = new ApprovalNode();
        node1.setNodeName("部门负责人审批");
        node1.setNodeOrder(order++);
        node1.setApproverId(approverResolver.resolveDeptManager(deptId));
        node1.setStatus(NodeStatus.PENDING.getCode());
        nodes.add(node1);

        // Node 2: HR负责人（必须）
        Long hrApproverId = approverResolver.resolveHrApprover();
        if (hrApproverId != null) {
            ApprovalNode node2 = new ApprovalNode();
            node2.setNodeName("HR负责人审批");
            node2.setNodeOrder(order);
            node2.setApproverId(hrApproverId);
            node2.setStatus(NodeStatus.PENDING.getCode());
            nodes.add(node2);
        }

        return nodes;
    }

    @Override
    public ApprovalBizType supportedBizType() {
        return ApprovalBizType.RESIGNATION;
    }
}
