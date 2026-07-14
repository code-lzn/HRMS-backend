package com.limou.hrms.builder;

import com.limou.hrms.mapper.DepartmentMapper;
import com.limou.hrms.mapper.EmployeeWorkInfoMapper;
import com.limou.hrms.mapper.LeaveRequestMapper;
import com.limou.hrms.model.entity.ApprovalNode;
import com.limou.hrms.model.entity.Department;
import com.limou.hrms.model.entity.EmployeeWorkInfo;
import com.limou.hrms.model.entity.LeaveRequest;
import com.limou.hrms.model.enums.ApprovalBizType;
import com.limou.hrms.model.enums.NodeStatus;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 请假审批节点构建器：Node1=直接上级 → Node2（若 leave_days > 3）部门负责人
 */
@Component
public class LeaveNodeBuilder implements ApprovalNodeBuilder {

    @Resource
    private LeaveRequestMapper leaveRequestMapper;
    @Resource
    private EmployeeWorkInfoMapper employeeWorkInfoMapper;
    @Resource
    private DepartmentMapper departmentMapper;

    @Override
    public List<ApprovalNode> build(Long bizId, Long applicantId) {
        LeaveRequest request = leaveRequestMapper.selectById(bizId);
        if (request == null) {
            throw new IllegalArgumentException("请假申请不存在: " + bizId);
        }

        // 查员工工作信息
        EmployeeWorkInfo workInfo = employeeWorkInfoMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<EmployeeWorkInfo>()
                        .eq("employee_id", request.getEmployeeId())
        );
        if (workInfo == null) {
            throw new IllegalArgumentException("员工工作信息不存在");
        }

        List<ApprovalNode> nodes = new ArrayList<>();
        int order = 1;

        // Node 1: 直接上级
        Long approverId;
        if (workInfo.getDirectReportId() != null) {
            approverId = workInfo.getDirectReportId();
        } else {
            // 若直接上级为空，回退到部门负责人
            Department dept = departmentMapper.selectById(workInfo.getDepartmentId());
            if (dept == null || dept.getManagerId() == null) {
                throw new IllegalArgumentException("部门或部门负责人不存在");
            }
            approverId = dept.getManagerId();
        }

        ApprovalNode node1 = new ApprovalNode();
        node1.setNodeName("直接上级审批");
        node1.setNodeOrder(order++);
        node1.setApproverId(approverId);
        node1.setStatus(NodeStatus.PENDING.getCode());
        nodes.add(node1);

        // Node 2: 若请假天数 > 3，加部门负责人节点
        if (request.getLeaveDays().compareTo(new BigDecimal("3")) > 0) {
            Department dept = departmentMapper.selectById(workInfo.getDepartmentId());
            if (dept == null || dept.getManagerId() == null) {
                throw new IllegalArgumentException("部门或部门负责人不存在");
            }

            ApprovalNode node2 = new ApprovalNode();
            node2.setNodeName("部门负责人审批");
            node2.setNodeOrder(order);
            node2.setApproverId(dept.getManagerId());
            node2.setStatus(NodeStatus.PENDING.getCode());
            nodes.add(node2);
        }

        return nodes;
    }

    @Override
    public ApprovalBizType supportedBizType() {
        return ApprovalBizType.LEAVE;
    }
}
