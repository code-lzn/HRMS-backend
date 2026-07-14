package com.limou.hrms.builder;

import com.limou.hrms.mapper.*;
import com.limou.hrms.model.entity.*;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.enums.NodeStatus;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 入职审批节点构建器：Node1=部门负责人 → Node2=HR负责人
 */
@Component
public class OnboardingNodeBuilder implements ApprovalNodeBuilder {

    @Resource
    private OnboardingApplicationMapper onboardingApplicationMapper;
    @Resource
    private DepartmentMapper departmentMapper;
    @Resource
    private EmployeeMapper employeeMapper;

    @Override
    public List<ApprovalNode> build(Long bizId, Long applicantId) {
        // 1. 查入职申请获取部门ID
        OnboardingApplication app = onboardingApplicationMapper.selectById(bizId);
        if (app == null) {
            throw new IllegalArgumentException("入职申请不存在: " + bizId);
        }

        // 2. 查部门获取部门负责人
        Department dept = departmentMapper.selectById(app.getDepartmentId());
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

        // Node 2: HR负责人 — 查 employee 表中 user_role='hr' 对应的员工
        // 通过 EmployeeMapper 查询所有 HR 员工（需要用 SQL 查询）
        // 简化：用 EmployeeMapper + 传入的 applicantId 查同部门HR
        // 实际通过 UserMapper 查 user_role='hr' 的 user，再映射到 employee
        // 第一版简化：直接用 applicantId（HR本人）作为 HR负责人审批节点
        // TODO 后续改为查 user_role='hr' 的用户列表
        ApprovalNode node2 = new ApprovalNode();
        node2.setNodeName("HR负责人审批");
        node2.setNodeOrder(order);
        node2.setApproverId(resolveHrApprover());
        node2.setStatus(NodeStatus.PENDING.getCode());
        nodes.add(node2);

        return nodes;
    }

    /**
     * 查找HR负责人。查 user 表 user_role='hr'，映射到 employee 表。
     * 若无 HR 员工记录，回退返回 1L（admin）。
     */
    private Long resolveHrApprover() {
        // 通过查 employee 表关联 user 角色
        List<Employee> hrEmployees = employeeMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Employee>()
                        .inSql("user_id", "SELECT id FROM user WHERE user_role = 'hr' AND is_deleted = 0")
                        .eq("is_deleted", 0)
                        .last("LIMIT 1")
        );
        if (!hrEmployees.isEmpty()) {
            return hrEmployees.get(0).getId();
        }
        // 回退：返回第一个员工
        List<Employee> all = employeeMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Employee>()
                        .eq("is_deleted", 0)
                        .last("LIMIT 1")
        );
        if (!all.isEmpty()) {
            return all.get(0).getId();
        }
        return 1L; // 最终回退
    }

    @Override
    public ApprovalBizType supportedBizType() {
        return ApprovalBizType.ONBOARDING;
    }
}
