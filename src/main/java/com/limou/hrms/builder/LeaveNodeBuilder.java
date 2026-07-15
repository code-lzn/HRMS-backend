package com.limou.hrms.builder;

import com.limou.hrms.mapper.LeaveRequestMapper;
import com.limou.hrms.model.entity.ApprovalNode;
import com.limou.hrms.model.entity.LeaveRequest;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.enums.NodeStatus;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 请假审批节点构建器 — 按 PRD 6.3.4 规则动态路由：
 *   年假/调休 ≤ 3天: 直接上级
 *   年假/调休 > 3天: 直接上级 → 部门负责人
 *   病假/事假 ≤ 1天: 直接上级
 *   病假/事假 > 1天: 直接上级 → 部门负责人
 *   婚假/产假/丧假:   直接上级 → HR备案（无需二审）
 */
@Component
public class LeaveNodeBuilder implements ApprovalNodeBuilder {

    private static final int ANNUAL = 1;
    private static final int SICK = 2;
    private static final int PERSONAL = 3;
    private static final int MARRIAGE = 4;
    private static final int MATERNITY = 5;
    private static final int FUNERAL = 6;
    private static final int COMP_TIME = 7;

    @Resource
    private LeaveRequestMapper leaveRequestMapper;
    @Resource
    private ApproverResolver approverResolver;

    @Override
    public List<ApprovalNode> build(Long bizId, Long applicantId) {
        LeaveRequest request = leaveRequestMapper.selectById(bizId);
        if (request == null) {
            throw new IllegalArgumentException("请假申请不存在: " + bizId);
        }

        List<ApprovalNode> nodes = new ArrayList<>();
        int order = 1;

        // Node 1: 直接上级（所有类型都需要）
        ApprovalNode node1 = new ApprovalNode();
        node1.setNodeName("直接上级审批");
        node1.setNodeOrder(order++);
        node1.setApproverId(approverResolver.resolveDirectLeader(request.getEmployeeId()));
        node1.setStatus(NodeStatus.PENDING.getCode());
        nodes.add(node1);

        int leaveType = request.getLeaveType();
        BigDecimal leaveDays = request.getLeaveDays();

        boolean needDeptHead = false;
        boolean needHrRecord = false;

        if (leaveType == ANNUAL || leaveType == COMP_TIME) {
            needDeptHead = leaveDays.compareTo(new BigDecimal("3")) > 0;
        } else if (leaveType == SICK || leaveType == PERSONAL) {
            needDeptHead = leaveDays.compareTo(BigDecimal.ONE) > 0;
        } else if (leaveType == MARRIAGE || leaveType == MATERNITY || leaveType == FUNERAL) {
            needHrRecord = true;
        }

        if (needDeptHead) {
            Long deptId = approverResolver.resolveDepartmentId(request.getEmployeeId());
            ApprovalNode node2 = new ApprovalNode();
            node2.setNodeName("部门负责人审批");
            node2.setNodeOrder(order++);
            node2.setApproverId(approverResolver.resolveDeptManager(deptId));
            node2.setStatus(NodeStatus.PENDING.getCode());
            nodes.add(node2);
        }

        if (needHrRecord) {
            Long hrApproverId = approverResolver.resolveHrApprover();
            if (hrApproverId != null) {
                ApprovalNode hrNode = new ApprovalNode();
                hrNode.setNodeName("HR备案");
                hrNode.setNodeOrder(order);
                hrNode.setApproverId(hrApproverId);
                hrNode.setStatus(NodeStatus.PENDING.getCode());
                nodes.add(hrNode);
            }
        }

        return nodes;
    }

    @Override
    public ApprovalBizType supportedBizType() {
        return ApprovalBizType.LEAVE;
    }
}
