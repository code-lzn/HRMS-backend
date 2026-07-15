package com.limou.hrms.builder;

import com.limou.hrms.mapper.SupplementCardRequestMapper;
import com.limou.hrms.model.entity.ApprovalNode;
import com.limou.hrms.model.entity.SupplementCardRequest;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.enums.NodeStatus;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 补卡审批节点构建器：Node1=直接上级
 */
@Component
public class SupplementCardNodeBuilder implements ApprovalNodeBuilder {

    @Resource
    private SupplementCardRequestMapper supplementCardRequestMapper;
    @Resource
    private ApproverResolver approverResolver;

    @Override
    public List<ApprovalNode> build(Long bizId, Long applicantId) {
        SupplementCardRequest request = supplementCardRequestMapper.selectById(bizId);
        if (request == null) {
            throw new IllegalArgumentException("补卡申请不存在: " + bizId);
        }

        List<ApprovalNode> nodes = new ArrayList<>();
        int order = 1;

        // 查员工工作信息获取直接上级
        // Node 1: 直接上级
        ApprovalNode node1 = new ApprovalNode();
        node1.setNodeName("直接上级审批");
        node1.setNodeOrder(order);
        node1.setApproverId(approverResolver.resolveDirectLeader(request.getEmployeeId()));
        node1.setStatus(NodeStatus.PENDING.getCode());
        nodes.add(node1);

        return nodes;
    }

    @Override
    public ApprovalBizType supportedBizType() {
        return ApprovalBizType.CARD_REPLENISH;
    }
}
