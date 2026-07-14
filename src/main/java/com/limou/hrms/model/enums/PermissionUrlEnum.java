package com.limou.hrms.model.enums;

import com.limou.hrms.constant.PermissionConstant;
import lombok.Getter;

/**
 * 权限 URL 映射枚举 —— 每个枚举值代表一条「URL 模式 → 所需权限码」的映射规则
 * <p>
 * 基于项目中所有现有 Controller 的真实接口路径（context-path=/api）。
 * 匹配规则：精确路径在前，通配路径在后；命中第一个即停止。
 */
@Getter
public enum PermissionUrlEnum {

    // ==================== 角色管理 —— 通配兜底 ====================
    ROLE_ALL("/api/role/**",               PermissionConstant.ROLE_MANAGE, "角色管理全部操作"),

    // ==================== 组织架构 —— 通配兜底 ====================
    DEPT_ALL("/api/departments/**",          PermissionConstant.ORG_MANAGE, "部门管理全部操作"),
    POS_ALL("/api/positions/**",             PermissionConstant.ORG_MANAGE, "职位管理全部操作"),

    // ==================== 员工管理（已实现 + 规划中） ====================
    EMPLOYEE_LIST("/api/employee/list",          PermissionConstant.EMPLOYEE_LIST, "员工列表"),
    EMPLOYEE_ADD("/api/employee/add",            PermissionConstant.EMPLOYEE_ADD, "新增员工"),
    EMPLOYEE_EDIT("/api/employee/edit",          PermissionConstant.EMPLOYEE_EDIT, "编辑员工"),
    EMPLOYEE_DELETE("/api/employee/delete",      PermissionConstant.EMPLOYEE_DELETE, "删除员工"),
    EMPLOYEE_PROFILE_UPDATE("/api/employee/profileUpdate", PermissionConstant.EMPLOYEE_EDIT, "编辑员工档案"),
    EMPLOYEE_DETAIL("/api/employee/detail",      PermissionConstant.EMPLOYEE_DETAIL, "员工详情"),

    // ==================== 用户管理 ====================
    USER_ADD("/api/user/add",               PermissionConstant.ROLE_MANAGE, "新增用户"),
    USER_DELETE("/api/user/delete",         PermissionConstant.ROLE_MANAGE, "删除用户"),
    USER_UPDATE("/api/user/update",         PermissionConstant.ROLE_MANAGE, "更新用户"),
    USER_GET("/api/user/get",               PermissionConstant.ROLE_MANAGE, "查询用户"),
    USER_GET_VO("/api/user/get/vo",         PermissionConstant.ROLE_MANAGE, "查询用户VO"),
    USER_LIST_PAGE("/api/user/list/page",   PermissionConstant.ROLE_MANAGE, "用户分页"),
    USER_LIST_PAGE_VO("/api/user/list/page/vo", PermissionConstant.ROLE_MANAGE, "用户分页VO"),

    // ==================== 薪资管理（已实现 + 规划中） ====================
    SALARY_LIST("/api/salary/list",              PermissionConstant.SALARY_LIST, "薪资列表"),
    SALARY_VIEW("/api/salary/view",              PermissionConstant.SALARY_VIEW, "薪资查看"),
    SALARY_AUDIT("/api/salary/audit",            PermissionConstant.SALARY_AUDIT, "薪资审核"),
    SALARY_SLIP_DETAIL("/api/salary/slip/**",    PermissionConstant.SALARY_VIEW, "工资条详情"),
    SALARY_TREND("/api/salary/trend",            PermissionConstant.SALARY_VIEW, "薪资趋势"),

    // ==================== 考勤管理（规划中） ====================
    ATTENDANCE_LIST("/api/attendance/list",      PermissionConstant.ATTENDANCE_LIST, "考勤列表"),
    ATTENDANCE_MANAGE("/api/attendance/manage",  PermissionConstant.ATTENDANCE_MANAGE, "考勤管理"),

    // ==================== 审批（已实现 + 规划中） ====================
    APPROVAL_PROCESS("/api/approval/process",         PermissionConstant.APPROVAL_PROCESS, "审批处理"),
    LEAVE_APPROVE("/api/attendance/leave/approve",    PermissionConstant.APPROVAL_PROCESS, "请假审批"),
    MAKEUP_APPROVE("/api/attendance/makeup/approve",  PermissionConstant.APPROVAL_PROCESS, "补卡审批"),
    LEAVE_CANCEL("/api/attendance/leave/cancel/**",   PermissionConstant.APPROVAL_PROCESS, "销假"),

    // ==================== 系统管理（规划中） ====================
    SYSTEM_CONFIG("/api/system/config",    PermissionConstant.SYSTEM_CONFIG, "系统配置"),
    SYSTEM_BACKUP("/api/system/backup",    PermissionConstant.SYSTEM_BACKUP, "数据备份"),

    ;

    /** AntPathMatcher 匹配用的 URL 模式 */
    private final String urlPattern;

    /** 访问该 URL 所需的权限码 */
    private final String permissionCode;

    /** 接口功能描述 */
    private final String description;

    PermissionUrlEnum(String urlPattern, String permissionCode, String description) {
        this.urlPattern = urlPattern;
        this.permissionCode = permissionCode;
        this.description = description;
    }
}
