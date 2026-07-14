package com.limou.hrms.builder;

import com.limou.hrms.mapper.EmployeeMapper;
import com.limou.hrms.mapper.SalaryBatchMapper;
import com.limou.hrms.model.entity.ApprovalNode;
import com.limou.hrms.model.entity.Employee;
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
    private EmployeeMapper employeeMapper;

    @Override
    public List<ApprovalNode> build(Long bizId, Long applicantId) {
        SalaryBatch batch = salaryBatchMapper.selectById(bizId);
        if (batch == null) {
            throw new IllegalArgumentException("薪资批次不存在: " + bizId);
        }

        List<ApprovalNode> nodes = new ArrayList<>();
        int order = 1;

        // Node 1: 财务专员
        ApprovalNode node1 = new ApprovalNode();
        node1.setNodeName("财务专员审批");
        node1.setNodeOrder(order);
        node1.setApproverId(resolveFinanceApprover());
        node1.setStatus(NodeStatus.PENDING.getCode());
        nodes.add(node1);

        return nodes;
    }

    /**
     * 查找财务专员。查 user 表 user_role='finance'，映射到 employee 表。
     */
    private Long resolveFinanceApprover() {
        List<Employee> financeEmployees = employeeMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Employee>()
                        .inSql("user_id", "SELECT id FROM user WHERE user_role = 'finance' AND is_deleted = 0")
                        .eq("is_deleted", 0)
                        .last("LIMIT 1")
        );
        if (!financeEmployees.isEmpty()) {
            return financeEmployees.get(0).getId();
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
        return ApprovalBizType.SALARY_BATCH;
    }
}
