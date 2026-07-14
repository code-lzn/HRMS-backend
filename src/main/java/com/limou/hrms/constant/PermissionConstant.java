package com.limou.hrms.constant;

import java.util.Arrays;
import java.util.List;

/**
 * 权限编码常量
 */
public interface PermissionConstant {

    // ========== 超级权限 ==========
    /** 超级管理员通配符 */
    String SUPER_ADMIN = "*:*:*";

    // ========== 员工管理 ==========
    String EMPLOYEE_LIST = "employee:list";
    String EMPLOYEE_ADD = "employee:add";
    String EMPLOYEE_EDIT = "employee:edit";
    String EMPLOYEE_DELETE = "employee:delete";
    String EMPLOYEE_DETAIL = "employee:detail";

    // ========== 薪资管理 ==========
    String SALARY_LIST = "salary:list";
    String SALARY_VIEW = "salary:view";
    String SALARY_AUDIT = "salary:audit";

    // ========== 审批管理 ==========
    String APPROVAL_PROCESS = "approval:process";

    // ========== 考勤管理 ==========
    String ATTENDANCE_CLOCK = "attendance:clock";
    String ATTENDANCE_LIST = "attendance:list";
    String ATTENDANCE_MANAGE = "attendance:manage";

    // ========== 组织架构 ==========
    String ORG_MANAGE = "org:manage";

    // ========== 系统管理 ==========
    String SYSTEM_CONFIG = "system:config";
    String SYSTEM_BACKUP = "system:backup";
    String ROLE_MANAGE = "role:manage";

    /**
     * 所有权限编码列表（供前端权限分配界面使用）
     */
    List<String> ALL_CODES = Arrays.asList(
            // 员工管理
            EMPLOYEE_LIST, EMPLOYEE_ADD, EMPLOYEE_EDIT, EMPLOYEE_DELETE, EMPLOYEE_DETAIL,
            // 薪资管理
            SALARY_LIST, SALARY_VIEW, SALARY_AUDIT,
            // 审批管理
            APPROVAL_PROCESS,
            // 考勤管理
            ATTENDANCE_CLOCK, ATTENDANCE_LIST, ATTENDANCE_MANAGE,
            // 组织架构
            ORG_MANAGE,
            // 系统管理
            SYSTEM_CONFIG, SYSTEM_BACKUP, ROLE_MANAGE
    );

    /**
     * 权限分组（供前端渲染权限树/分组）
     */
    List<PermissionGroup> GROUPS = Arrays.asList(
            new PermissionGroup("员工管理", Arrays.asList(EMPLOYEE_LIST, EMPLOYEE_ADD, EMPLOYEE_EDIT, EMPLOYEE_DELETE, EMPLOYEE_DETAIL)),
            new PermissionGroup("薪资管理", Arrays.asList(SALARY_LIST, SALARY_VIEW, SALARY_AUDIT)),
            new PermissionGroup("审批管理", Arrays.asList(APPROVAL_PROCESS)),
            new PermissionGroup("考勤管理", Arrays.asList(ATTENDANCE_CLOCK, ATTENDANCE_LIST, ATTENDANCE_MANAGE)),
            new PermissionGroup("组织架构", Arrays.asList(ORG_MANAGE)),
            new PermissionGroup("系统管理", Arrays.asList(SYSTEM_CONFIG, SYSTEM_BACKUP, ROLE_MANAGE))
    );

    class PermissionGroup {
        private final String groupName;
        private final List<String> codes;

        public PermissionGroup(String groupName, List<String> codes) {
            this.groupName = groupName;
            this.codes = codes;
        }

        public String getGroupName() { return groupName; }
        public List<String> getCodes() { return codes; }
    }
}
