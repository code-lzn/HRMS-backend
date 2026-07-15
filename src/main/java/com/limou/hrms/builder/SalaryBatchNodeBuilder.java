package com.limou.hrms.builder;

import com.limou.hrms.mapper.SalaryBatchMapper;
import com.limou.hrms.model.entity.ApprovalNode;
import com.limou.hrms.model.entity.SalaryBatch;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.enums.NodeStatus;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 薪资批次审批节点构建器：Node1=财务专员
 */
@Component
public class SalaryBatchNodeBuilder implements ApprovalNodeBuilder {

    @Resource
    private SalaryBatchMapper salaryBatchMapper;
    @Resource
    private ApproverResolver approverResolver;

    @Override
    public List<ApprovalNode> build(Long bizId, Long applicantId) {
        SalaryBatch batch = salaryBatchMapper.selectById(bizId);
        if (batch == null) {
            throw new IllegalArgumentException("薪资批次不存在: " + bizId);
        }

        List<ApprovalNode> nodes = new ArrayList<>();

        int order = 1;

        // Node 1: 财务专员
        Long financeApproverId = approverResolver.resolveFinanceApprover();
        if (financeApproverId != null) {
            ApprovalNode node1 = new ApprovalNode();
            node1.setNodeName("财务专员审批");
            node1.setNodeOrder(order++);
            node1.setApproverId(financeApproverId);
            node1.setStatus(NodeStatus.PENDING.getCode());
            nodes.add(node1);
        }

        // Node 2: 老板（可选，PRD 标记为 [老板]）
        Long bossApproverId = approverResolver.resolveBossApprover();
        if (bossApproverId != null) {
            ApprovalNode bossNode = new ApprovalNode();
            bossNode.setNodeName("老板审批");
            bossNode.setNodeOrder(order);
            bossNode.setApproverId(bossApproverId);
            bossNode.setStatus(NodeStatus.PENDING.getCode());
            nodes.add(bossNode);
        }

        return nodes;
    }

    @Override
    public ApprovalBizType supportedBizType() {
        return ApprovalBizType.SALARY_BATCH;
    }
}
