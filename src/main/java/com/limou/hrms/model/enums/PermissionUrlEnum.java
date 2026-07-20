package com.limou.hrms.model.enums;

import com.limou.hrms.constant.PermissionConstant;
import lombok.Getter;

/**
 * 权限 URL 映射枚举 —— 每个枚举值代表一条「URL 模式 → 所需权限码」的映射规则
 * <p>
 * 基于项目中所有现有 Controller 的真实接口路径（context-path=/api）。
 * 匹配规则：精确路径在前，通配路径在后；命中第一个即停止。
 * <p>
 * 设计原则：
 * - 仅拦截"管理类"接口（需要特定权限码才能访问）
 * - 自服务接口（如查看个人信息、工资条、个人考勤等）不在此处拦截，
 *   由 Service 层根据用户数据范围（ALL/DEPARTMENT/SELF）进行过滤
 */
@Getter
public enum PermissionUrlEnum {

    // ==================== 角色管理 —— 通配兜底 ====================
    ROLE_ALL("/api/role/**",               PermissionConstant.ROLE_MANAGE, "角色管理全部操作"),

    // ==================== 组织架构 —— 读写分离（精确路径优先） ====================
    // 写操作 — 需 org:manage
    DEPT_ADD("/api/departments/add",          PermissionConstant.ORG_MANAGE, "新增部门"),
    DEPT_UPDATE("/api/departments/update",    PermissionConstant.ORG_MANAGE, "更新部门"),
    DEPT_DELETE("/api/departments/delete",    PermissionConstant.ORG_MANAGE, "删除部门"),
    DEPT_MERGE("/api/departments/merge",      PermissionConstant.ORG_MANAGE, "合并部门"),
    POS_ADD("/api/positions/add",             PermissionConstant.ORG_MANAGE, "新增职位"),
    POS_UPDATE("/api/positions/update",        PermissionConstant.ORG_MANAGE, "更新职位"),
    POS_DELETE("/api/positions/delete",        PermissionConstant.ORG_MANAGE, "删除职位"),
    // 读操作 — 有 employee:list 即可（部门树、职位列表为管理员工时的参考数据）
    DEPT_TREE("/api/departments/tree",         PermissionConstant.EMPLOYEE_LIST, "部门树"),
    DEPT_DETAIL("/api/departments/detail",     PermissionConstant.EMPLOYEE_LIST, "部门详情"),
    POS_LIST("/api/positions/list",            PermissionConstant.EMPLOYEE_LIST, "职位列表"),
    POS_SEQUENCES("/api/positions/sequences",  PermissionConstant.EMPLOYEE_LIST, "序列职级对照"),

    // ==================== 员工管理（管理类接口） ====================
    EMPLOYEE_LIST("/api/employee/list",          PermissionConstant.EMPLOYEE_LIST, "员工列表"),
    EMPLOYEE_ADD("/api/employee/add",            PermissionConstant.EMPLOYEE_ADD, "新增员工"),
    EMPLOYEE_EDIT("/api/employee/update",        PermissionConstant.EMPLOYEE_EDIT, "编辑员工"),
    EMPLOYEE_DELETE("/api/employee/delete",      PermissionConstant.EMPLOYEE_DELETE, "删除员工"),
    EMPLOYEE_DETAIL("/api/employee/detail",      PermissionConstant.EMPLOYEE_DETAIL, "员工详情"),
    EMPLOYEE_CHANGE_LOGS("/api/employee/change-logs", PermissionConstant.EMPLOYEE_DETAIL, "员工变更历史"),
    // 注意：/api/employee/profile 和 /api/employee/profileUpdate 为自服务接口，不拦截

    // ==================== 用户管理 ====================
    USER_ADD("/api/user/add",               PermissionConstant.ROLE_MANAGE, "新增用户"),
    USER_DELETE("/api/user/delete",         PermissionConstant.ROLE_MANAGE, "删除用户"),
    USER_UPDATE_STATUS("/api/user/update/status", PermissionConstant.ROLE_MANAGE, "更新用户状态"),
    USER_UPDATE("/api/user/update",         PermissionConstant.ROLE_MANAGE, "更新用户"),
    USER_GET("/api/user/get",               PermissionConstant.ROLE_MANAGE, "查询用户"),
    USER_GET_VO("/api/user/get/vo",         PermissionConstant.ROLE_MANAGE, "查询用户VO"),
    USER_LIST_PAGE("/api/user/list/page",   PermissionConstant.ROLE_MANAGE, "用户分页"),
    USER_LIST_PAGE_VO("/api/user/list/page/vo", PermissionConstant.ROLE_MANAGE, "用户分页VO"),

    // ==================== 薪资管理（管理端 — 精确路径优先） ====================
    // 审核操作（audit 权限 — 仅财务）
    SALARY_BATCH_APPROVE("/api/salary-manage/batches/*/approve",   PermissionConstant.SALARY_AUDIT, "薪资批次审批通过"),
    SALARY_BATCH_REJECT("/api/salary-manage/batches/*/reject",    PermissionConstant.SALARY_AUDIT, "薪资批次驳回"),
    SALARY_BATCH_MARK_PAID("/api/salary-manage/batches/*/mark-paid", PermissionConstant.SALARY_AUDIT, "薪资批次标记已发放"),
    // 核算操作（list 权限 — HR/财务）
    SALARY_BATCH_SUBMIT("/api/salary-manage/batches/*/submit",    PermissionConstant.SALARY_LIST, "薪资批次提交审批"),
    SALARY_BATCH_CALCULATE("/api/salary-manage/batches/*/calculate", PermissionConstant.SALARY_LIST, "薪资批次核算"),
    SALARY_BATCH_ADJUST("/api/salary-manage/batches/*/adjust",    PermissionConstant.SALARY_LIST, "薪资批次调整"),
    // 薪资管理其余接口（list 权限兜底）
    SALARY_MANAGE_ALL("/api/salary-manage/**",                     PermissionConstant.SALARY_LIST, "薪资管理全部操作"),
    // 注意：/api/salary/slips、/api/salary/slip/{id}、/api/salary/trend 为自服务接口（我的工资条），不拦截

    // ==================== 考勤管理（管理类接口） ====================
    ATTENDANCE_LIST("/api/attendance/list",      PermissionConstant.ATTENDANCE_LIST, "考勤列表"),
    ATTENDANCE_MANAGE("/api/attendance/manage",  PermissionConstant.ATTENDANCE_MANAGE, "考勤管理"),
    // HR端考勤管理接口
    HR_ATTENDANCE_LIST("/api/hr/attendance/list",        PermissionConstant.ATTENDANCE_LIST, "HR考勤列表"),
    HR_ATTENDANCE_CREATE("/api/hr/attendance/create",    PermissionConstant.ATTENDANCE_MANAGE, "HR新增考勤"),
    HR_ATTENDANCE_UPDATE("/api/hr/attendance/update",    PermissionConstant.ATTENDANCE_MANAGE, "HR编辑考勤"),
    HR_ATTENDANCE_DELETE("/api/hr/attendance/delete/**", PermissionConstant.ATTENDANCE_MANAGE, "HR删除考勤"),
    HR_ATTENDANCE_BATCH_DELETE("/api/hr/attendance/batch-delete", PermissionConstant.ATTENDANCE_MANAGE, "HR批量删除考勤"),
    // 注意：/api/attendance/punch、/api/attendance/today、/api/attendance/calendar、
    //       /api/attendance/records 为自服务接口（我的考勤），不拦截

    // ==================== 审批（管理类接口 — 精确路径优先） ====================
    // 审批操作（需审批权限）
    APPROVAL_PENDING("/api/approval/pending",              PermissionConstant.APPROVAL_PROCESS, "待审批列表"),
    APPROVAL_APPROVE("/api/approval/approve",              PermissionConstant.APPROVAL_PROCESS, "审批通过"),
    APPROVAL_REJECT("/api/approval/reject",                PermissionConstant.APPROVAL_PROCESS, "审批拒绝"),
    APPROVAL_TRANSFER("/api/approval/transfer",            PermissionConstant.APPROVAL_PROCESS, "审批转交"),
    APPROVAL_DETAIL("/api/approval/detail/**",             PermissionConstant.APPROVAL_PROCESS, "审批详情"),
    LEAVE_APPROVE("/api/attendance/leave/approve",         PermissionConstant.APPROVAL_PROCESS, "请假审批"),
    MAKEUP_APPROVE("/api/attendance/makeup/approve",       PermissionConstant.APPROVAL_PROCESS, "补卡审批"),
    // 注意：/api/approval/delegation/** 为自服务接口（我的委托），不拦截
    // 注意：/api/attendance/leave/cancel/{id} 为自服务接口（撤销本人的请假），不拦截
    // 注意：/api/attendance/leave/apply、/api/attendance/makeup/apply 为自服务接口，不拦截
    // 注意：/api/attendance/leave/my、/api/attendance/makeup/my 为自服务接口，不拦截
    // 注意：/api/attendance/leave/{id}/progress 为自服务接口，不拦截

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
