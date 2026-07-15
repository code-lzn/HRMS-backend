package com.limou.hrms.builder;

import com.limou.hrms.mapper.*;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.enums.NodeStatus;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 转正审批节点构建器：Node1=部门负责人 → Node2=HR负责人（必须）
 */
@Component
public class ProbationNodeBuilder implements ApprovalNodeBuilder {

    @Resource
    private ProbationApplicationMapper probationApplicationMapper;
    @Resource
    private EmployeeWorkInfoMapper employeeWorkInfoMapper;
    @Resource
    private DepartmentMapper departmentMapper;
    @Resource
    private ApproverResolver approverResolver;

    @Override
    public List<ApprovalNode> build(Long bizId, Long applicantId) {
        ProbationApplication app = probationApplicationMapper.selectById(bizId);
        if (app == null) {
            throw new IllegalArgumentException("转正申请不存在: " + bizId);
        }

        EmployeeWorkInfo workInfo = employeeWorkInfoMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<EmployeeWorkInfo>()
                        .eq("employee_id", app.getEmployeeId()));
        if (workInfo == null) {
            throw new IllegalArgumentException("员工工作信息不存在");
        }

        Department dept = departmentMapper.selectById(workInfo.getDepartmentId());
        if (dept == null || dept.getManagerId() == null) {
            throw new IllegalArgumentException("部门或部门负责人不存在");
        }

        List<ApprovalNode> nodes = new ArrayList<>();
        int order = 1;

        // Node 1: 部门负责人
        ApprovalNode node1 = new ApprovalNode();
        node1.setNodeName("部门负责人审批");
        node1.setNodeOrder(order++);
        node1.setApproverId(dept.getManagerId());
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
        return ApprovalBizType.PROBATION;
    }
}
