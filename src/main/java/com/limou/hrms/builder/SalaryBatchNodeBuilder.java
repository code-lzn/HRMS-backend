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

        // Node 1: 财务专员
        Long financeApproverId = approverResolver.resolveFinanceApprover();
        if (financeApproverId != null) {
            ApprovalNode node1 = new ApprovalNode();
            node1.setNodeName("财务专员审批");
            node1.setNodeOrder(1);
            node1.setApproverId(financeApproverId);
            node1.setStatus(NodeStatus.PENDING.getCode());
            nodes.add(node1);
        }

        return nodes;
    }

    @Override
    public ApprovalBizType supportedBizType() {
        return ApprovalBizType.SALARY_BATCH;
    }
}
