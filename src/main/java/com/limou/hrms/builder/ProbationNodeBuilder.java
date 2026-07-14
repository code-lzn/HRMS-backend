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
 * 转正审批节点构建器：Node1=部门负责人 → Node2=HR负责人
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
    private EmployeeMapper employeeMapper;

    @Override
    public List<ApprovalNode> build(Long bizId, Long applicantId) {
        // 1. 查转正申请获取员工ID
        ProbationApplication app = probationApplicationMapper.selectById(bizId);
        if (app == null) {
            throw new IllegalArgumentException("转正申请不存在: " + bizId);
        }

        // 2. 查员工工作信息获取部门ID
        EmployeeWorkInfo workInfo = employeeWorkInfoMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<EmployeeWorkInfo>()
                        .eq("employee_id", app.getEmployeeId())
        );
        if (workInfo == null) {
            throw new IllegalArgumentException("员工工作信息不存在");
        }

        // 3. 查部门获取部门负责人
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

        // Node 2: HR负责人
        ApprovalNode node2 = new ApprovalNode();
        node2.setNodeName("HR负责人审批");
        node2.setNodeOrder(order);
        node2.setApproverId(resolveHrApprover());
        node2.setStatus(NodeStatus.PENDING.getCode());
        nodes.add(node2);

        return nodes;
    }

    private Long resolveHrApprover() {
        List<Employee> hrEmployees = employeeMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Employee>()
                        .inSql("user_id", "SELECT id FROM user WHERE user_role = 'hr' AND is_deleted = 0")
                        .eq("is_deleted", 0)
                        .last("LIMIT 1")
        );
        if (!hrEmployees.isEmpty()) {
            return hrEmployees.get(0).getId();
        }
        List<Employee> all = employeeMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Employee>()
                        .eq("is_deleted", 0)
                        .last("LIMIT 1")
        );
        if (!all.isEmpty()) {
            return all.get(0).getId();
        }
        return 1L;
    }

    @Override
    public ApprovalBizType supportedBizType() {
        return ApprovalBizType.PROBATION;
    }
}
