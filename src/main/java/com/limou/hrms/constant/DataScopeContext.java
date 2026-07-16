package com.limou.hrms.constant;

import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Set;

/**
 * 数据权限上下文 — 请求级，持有当前用户的数据权限信息
 */
@Component
@RequestScope
@Data
public class DataScopeContext {

    /** 当前登录用户 ID */
    private Long currentUserId;

    /** 当前用户角色值（如 "admin", "hr"） */
    private String currentRole;

    /** 当前用户的员工 ID（employee.id），可能为空 */
    private Long currentEmployeeId;

    /** 当前用户的部门 ID，部门主管时有值 */
    private Long currentDepartmentId;

    /** 当前用户管辖的部门 ID 集合（含子部门），dept_head 时有值 */
    private Set<Long> managedDepartmentIds;

    // ======================== 模块权限 ========================

    /** 员工档案数据范围 */
    public DataScopeEnum getEmployeeScope() {
        switch (currentRole) {
            case "admin":
            case "hr":
                return DataScopeEnum.ALL;
            case "dept_head":
                return DataScopeEnum.DEPT;
            case "finance":
            case "user":
                return DataScopeEnum.SELF;
            default:
                return DataScopeEnum.NONE;
        }
    }

    /** 薪资信息数据范围 */
    public DataScopeEnum getSalaryScope() {
        switch (currentRole) {
            case "hr":
            case "finance":
                return DataScopeEnum.ALL;
            case "user":
                return DataScopeEnum.SELF;
            default:
                // admin, dept_head 无权查看薪资
                return DataScopeEnum.NONE;
        }
    }

    /** 组织架构管理权限 — 仅 admin / hr */
    public DataScopeEnum canManageOrganization() {
        switch(currentRole){
            case "hr":
            case "admin":
                return DataScopeEnum.ALL;
            case "dept_head":
                return DataScopeEnum.DEPT;
            default:
                return DataScopeEnum.NONE;
        }
    }

    /** 考勤管理数据范围 */
    public DataScopeEnum getAttendanceScope() {
        switch (currentRole) {
            case "admin":
            case "hr":
                return DataScopeEnum.ALL;
            case "dept_head":
                return DataScopeEnum.DEPT;
            case "user":
                return DataScopeEnum.SELF;
            default:
                return DataScopeEnum.NONE;
        }
    }

    /** 审批管理数据范围 */
    public DataScopeEnum getApprovalScope() {
        switch (currentRole) {
            case "admin":
            case "hr":
                return DataScopeEnum.ALL;
            case "dept_head":
                return DataScopeEnum.DEPT;
            case "user":
                return DataScopeEnum.SELF;
            default:
                return DataScopeEnum.NONE;
        }
    }
}
