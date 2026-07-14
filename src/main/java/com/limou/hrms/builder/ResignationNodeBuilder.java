package com.limou.hrms.builder;

import com.limou.hrms.config.ApprovalConfig;
import com.limou.hrms.mapper.DepartmentMapper;
import com.limou.hrms.mapper.EmployeeWorkInfoMapper;
import com.limou.hrms.mapper.ResignationApplicationMapper;
import com.limou.hrms.model.entity.ApprovalNode;
import com.limou.hrms.model.entity.Department;
import com.limou.hrms.model.entity.EmployeeWorkInfo;
import com.limou.hrms.model.entity.ResignationApplication;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.enums.NodeStatus;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 离职审批节点构建器：Node1=部门负责人 → Node2=HR负责人（可配置开关）
 */
@Component
public class ResignationNodeBuilder implements ApprovalNodeBuilder {

    @Resource
    private ResignationApplicationMapper resignationApplicationMapper;
    @Resource
    private EmployeeWorkInfoMapper employeeWorkInfoMapper;
    @Resource
    private DepartmentMapper departmentMapper;
    @Resource
    private ApprovalConfig approvalConfig;
    @Resource
    private ApproverResolver approverResolver;

    @Override
    public List<ApprovalNode> build(Long bizId, Long applicantId) {
        ResignationApplication app = resignationApplicationMapper.selectById(bizId);
        if (app == null) {
            throw new IllegalArgumentException("离职申请不存在: " + bizId);
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

        // Node 2: HR负责人（可配置开关，默认开启）
        if (approvalConfig.getHrNode().isEnabled()) {
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

    @Override
    public ApprovalBizType supportedBizType() {
        return ApprovalBizType.RESIGNATION;
    }
}
