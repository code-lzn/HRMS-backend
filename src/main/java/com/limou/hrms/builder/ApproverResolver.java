package com.limou.hrms.builder;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.limou.hrms.mapper.DepartmentMapper;
import com.limou.hrms.mapper.EmployeeMapper;
import com.limou.hrms.mapper.UserMapper;
import com.limou.hrms.model.entity.Department;
import com.limou.hrms.model.entity.Employee;
import com.limou.hrms.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 审批人解析器 — 统一查询各类审批人  按角色找人
 */
@Component
@Slf4j
public class ApproverResolver {

    @Resource
    private DepartmentMapper departmentMapper;
    @Resource
    private EmployeeMapper employeeMapper;
    @Resource
    private UserMapper userMapper;

    /**
     * 查找 HR 负责人。查 user 表 user_role='hr' → 映射到 employee 表。
     * @return HR 负责人的 employee.id，若无则返回 null
     */
    public Long resolveHrApprover() {
        List<Employee> hrEmployees = employeeMapper.selectList(
                new QueryWrapper<Employee>()
                        .inSql("user_id", "SELECT id FROM user WHERE user_role = 'hr' AND is_deleted = 0")
                        .eq("is_deleted", 0)
                        .last("LIMIT 1"));
        if (!hrEmployees.isEmpty()) {
            return hrEmployees.get(0).getId();
        }
        log.warn("未找到 HR 负责人（user_role='hr' 且有关联 employee 记录）");
        return null;
    }

    /**
     * 查找财务专员。查 user 表 user_role='finance' → 映射到 employee 表。
     * @return 财务专员的 employee.id，若无则返回 null
     */
    public Long resolveFinanceApprover() {
        List<Employee> financeEmployees = employeeMapper.selectList(
                new QueryWrapper<Employee>()
                        .inSql("user_id", "SELECT id FROM user WHERE user_role = 'finance' AND is_deleted = 0")
                        .eq("is_deleted", 0)
                        .last("LIMIT 1"));
        if (!financeEmployees.isEmpty()) {
            return financeEmployees.get(0).getId();
        }
        log.warn("未找到财务专员（user_role='finance' 且有关联 employee 记录）");
        return null;
    }

    /**
     * 查找老板（公司负责人）。查 department 表 parent_id IS NULL 的根部门，
     * 取其 manager_id 作为老板的 employee.id。
     * @return 老板的 employee.id，若无根部门或未设负责人则返回 null
     */
    public Long resolveBossApprover() {
        List<Department> roots = departmentMapper.selectList(
                new QueryWrapper<Department>()
                        .isNull("parent_id")
                        .eq("is_deleted", 0)
                        .last("LIMIT 1"));
        if (roots.isEmpty() || roots.get(0).getManagerId() == null) {
            log.warn("未找到老板（根部门不存在或未设置负责人）");
            return null;
        }
        return roots.get(0).getManagerId();
    }

    /**
     * 获取员工姓名
     */
    public String getEmployeeName(Long employeeId) {
        if (employeeId == null) return null;
        Employee emp = employeeMapper.selectById(employeeId);
        if (emp == null) return null;
        User user = userMapper.selectById(emp.getUserId());
        return user != null ? user.getUserName() : emp.getEmployeeNo();
    }
}
