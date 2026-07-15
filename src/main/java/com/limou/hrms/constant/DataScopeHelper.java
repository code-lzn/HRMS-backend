package com.limou.hrms.constant;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.limou.hrms.model.entity.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Set;

/**
 * 数据权限查询辅助工具 — 根据 DataScopeContext 给 MyBatis-Plus QueryWrapper 注入数据范围条件
 */
@Component
@RequiredArgsConstructor
public class DataScopeHelper {

    @Resource
    private DataScopeContext dataScopeContext;

    // ======================== 员工档案 ========================

    /**
     * 员工档案数据过滤 — 给 Employee 表的查询注入数据范围
     * Employee 表通过 user_id 关联 user
     */
    public void applyEmployeeScope(QueryWrapper<Employee> wrapper) {
        DataScopeEnum scope = dataScopeContext.getEmployeeScope();
        Long currentUserId = dataScopeContext.getCurrentUserId();

        switch (scope) {
            case ALL:
                // 全量，不加过滤
                return;
            case DEPT: {
                // 本部门及下属部门 — 需要结合 EmployeeWorkInfo 做子查询
                Set<Long> deptIds = dataScopeContext.getManagedDepartmentIds();
                if (CollUtil.isEmpty(deptIds)) {
                    // 没有管辖部门，看自己
                    wrapper.eq("user_id", currentUserId);
                } else {
                    // 用子查询：找出 employee_id IN (SELECT employee_id FROM employee_work_info WHERE department_id IN deptIds)
                    wrapper.inSql("user_id",
                            "SELECT e.user_id FROM employee e " +
                            "INNER JOIN employee_work_info ewi ON e.id = ewi.employee_id " +
                            "WHERE ewi.department_id IN (" + joinIds(deptIds) + ")");
                }
                return;
            }
            case SELF:
            default:
                // 仅看自己
                wrapper.eq("user_id", currentUserId);
        }
    }

    // ======================== 考勤 ========================

    /**
     * 考勤记录数据过滤 — attendance_record 表通过 employee_id 关联 user.id
     */
    public void applyAttendanceScope(QueryWrapper<?> wrapper) {
        DataScopeEnum scope = dataScopeContext.getAttendanceScope();
        Long currentUserId = dataScopeContext.getCurrentUserId();

        switch (scope) {
            case ALL:
                return;
            case DEPT: {
                Set<Long> deptIds = dataScopeContext.getManagedDepartmentIds();
                if (CollUtil.isEmpty(deptIds)) {
                    wrapper.eq("employee_id", currentUserId);
                } else {
                    // attendance_record.employee_id 直接存 user.id
                    wrapper.inSql("employee_id",
                            "SELECT e.user_id FROM employee e " +
                            "INNER JOIN employee_work_info ewi ON e.id = ewi.employee_id " +
                            "WHERE ewi.department_id IN (" + joinIds(deptIds) + ")");
                }
                return;
            }
            case SELF:
            default:
                wrapper.eq("employee_id", currentUserId);
        }
    }

    // ======================== 薪资明细 ========================

    /**
     * 薪资明细数据过滤 — salary_detail 表通过 employee_id 关联 user.id
     */
    public void applySalaryDetailScope(QueryWrapper<?> wrapper) {
        DataScopeEnum scope = dataScopeContext.getSalaryScope();
        Long currentUserId = dataScopeContext.getCurrentUserId();

        switch (scope) {
            case ALL:
                return;
            case SELF:
                wrapper.eq("employee_id", currentUserId);
                return;
            case DEPT:
            case NONE:
            default:
                // 部门主管、管理员无权看薪资，兜底过滤
                wrapper.eq("employee_id", currentUserId);
        }
    }

    // ======================== 工具方法 ========================

    private String joinIds(Set<Long> ids) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Long id : ids) {
            if (i > 0) sb.append(",");
            sb.append(id);
            i++;
        }
        return sb.toString();
    }
}
