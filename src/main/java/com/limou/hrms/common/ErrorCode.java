package com.limou.hrms.common;

import lombok.Getter;

/**
 * 自定义错误码
 *
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "ok"),
    PARAMS_ERROR(40000, "请求参数错误"),
    NOT_LOGIN_ERROR(40100, "未登录"),
    NO_AUTH_ERROR(40101, "无权限"),
    NOT_FOUND_ERROR(40400, "请求数据不存在"),
    FORBIDDEN_ERROR(40300, "禁止访问"),
    SYSTEM_ERROR(50000, "系统内部异常"),
    OPERATION_ERROR(50001, "操作失败"),

    // 审批相关
    APPROVAL_INSTANCE_NOT_FOUND(40001, "审批实例不存在"),
    APPROVAL_NODE_NOT_FOUND(40002, "审批节点不存在"),
    APPROVAL_NODE_NOT_OWNER(40003, "该节点不属于当前用户"),
    APPROVAL_NODE_ALREADY_HANDLED(40004, "该节点已被处理"),
    APPROVAL_CANCEL_ONLY_FIRST_NODE(40005, "仅第一节点可撤回"),
    APPROVAL_NODE_TIMEOUT(40011, "审批已超时，无法操作"),

    // region 组织架构管理 (30xxx)
    DEPARTMENT_NOT_FOUND(30000, "部门不存在"),
    DEPARTMENT_NAME_DUPLICATE(30001, "部门名称已存在"),
    DEPARTMENT_CODE_DUPLICATE(30002, "部门编码已存在"),
    DEPARTMENT_HAS_CHILDREN(30003, "该部门下存在子部门，请先删除子部门"),
    DEPARTMENT_HAS_EMPLOYEES(30004, "该部门下有在职员工，请先转移员工"),
    DEPARTMENT_MAX_DEPTH_EXCEEDED(30005, "超过最大层级深度5级"),
    DEPARTMENT_SORT_ORDER_DUPLICATE(30006, "同级部门中排序序号重复"),
    DEPARTMENT_CIRCULAR_REF(30007, "不能将部门移动到自身或子部门下"),
    DEPARTMENT_PARENT_REQUIRED(30008, "必须指定上级部门，不允许创建根部门"),
    POSITION_NOT_FOUND(30010, "职位不存在"),
    POSITION_HAS_EMPLOYEES(30011, "该职位下有在职员工关联，请先调整员工职位"),
    POSITION_LEVEL_RANGE_INVALID(30012, "职级范围不合法"),
    POSITION_NAME_DUPLICATE(30013, "同一部门下职位名称重复"),
    // endregion

    // region 入转调离 — 入职管理 (310xx)
    ONBOARDING_NOT_FOUND(31001, "入职申请不存在"),
    ONBOARDING_DRAFT_ONLY(31002, "仅草稿状态可操作"),
    ONBOARDING_SUBMIT_DRAFT_ONLY(31003, "仅草稿状态可提交审批"),
    ONBOARDING_CANCEL_FIRST_NODE_ONLY(31004, "仅第一级审批节点可撤回申请"),
    ONBOARDING_CONFIRM_APPROVED_ONLY(31005, "仅\"已批准待入职\"状态可确认入职"),
    ONBOARDING_FIELDS_INCOMPLETE(31006, "入职申请必填字段不完整"),
    // endregion

    // region 入转调离 — 转正管理 (320xx)
    PROBATION_NOT_FOUND(32001, "转正申请不存在"),
    PROBATION_DRAFT_ONLY(32002, "仅草稿状态可操作"),
    PROBATION_SUBMIT_DRAFT_ONLY(32003, "仅草稿状态可提交审批"),
    PROBATION_CANCEL_FIRST_NODE_ONLY(32004, "仅第一级审批节点可撤回申请"),
    PROBATION_EMPLOYEE_NOT_PROBATION(32005, "员工不在试用期，无法发起转正"),
    PROBATION_HANDLE_REJECTED_ONLY(32006, "仅已拒绝状态可处理结果"),
    PROBATION_EXTEND_DATE_REQUIRED(32007, "延长试用需填写新的试用期结束日期"),
    // endregion

    // region 入转调离 — 调岗管理 (330xx)
    TRANSFER_NOT_FOUND(33001, "调岗申请不存在"),
    TRANSFER_DRAFT_ONLY(33002, "仅草稿状态可操作"),
    TRANSFER_SUBMIT_DRAFT_ONLY(33003, "仅草稿状态可提交审批"),
    TRANSFER_CANCEL_FIRST_NODE_ONLY(33004, "仅第一级审批节点可撤回申请"),
    TRANSFER_EMPLOYEE_NOT_ACTIVE(33005, "员工在职状态不允许调岗"),
    TRANSFER_DEPT_SAME(33006, "调岗前后部门不能相同"),
    // endregion

    // region 入转调离 — 离职管理 (340xx)
    RESIGNATION_NOT_FOUND(34001, "离职申请不存在"),
    RESIGNATION_DRAFT_ONLY(34002, "仅草稿状态可操作"),
    RESIGNATION_SUBMIT_DRAFT_ONLY(34003, "仅草稿状态可提交审批"),
    RESIGNATION_CANCEL_FIRST_NODE_ONLY(34004, "仅第一级审批节点可撤回申请"),
    RESIGNATION_EMPLOYEE_NOT_ACTIVE(34005, "员工在职状态不允许离职"),
    RESIGNATION_DATE_BEFORE_TODAY(34006, "离职日期不能早于今天");
    // endregion


    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
