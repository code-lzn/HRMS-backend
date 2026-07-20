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

    // region 考勤管理 (31xxx)
    ATTENDANCE_GROUP_NOT_FOUND(61000, "考勤组不存在"),
    ATTENDANCE_GROUP_RULE_DUPLICATE(61001, "该适用规则已存在于其他考勤组中"),
    ATTENDANCE_GROUP_RULE_DEPT_OUT_OF_SCOPE(61002, "所选部门/职位/个人不在管辖范围内"),
    ATTENDANCE_GROUP_RULE_POSITION_NO_EMPLOYEE(61003, "该全公司通用职位在管辖范围内无对应员工"),
    ATTENDANCE_GROUP_HAS_EMPLOYEES(61004, "该考勤组下仍有适用人员，请先调整人员归属"),
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
    TRANSFER_EMPLOYEE_NOT_ACTIVE(33005, "仅试用期或正式员工可调岗"),
    TRANSFER_DEPT_SAME(33006, "调岗前后部门不能相同"),
    // endregion

    // region 入转调离 — 离职管理 (340xx)
    RESIGNATION_NOT_FOUND(34001, "离职申请不存在"),
    RESIGNATION_DRAFT_ONLY(34002, "仅草稿状态可操作"),
    RESIGNATION_SUBMIT_DRAFT_ONLY(34003, "仅草稿状态可提交审批"),
    RESIGNATION_CANCEL_FIRST_NODE_ONLY(34004, "仅第一级审批节点可撤回申请"),
    RESIGNATION_EMPLOYEE_NOT_ACTIVE(34005, "仅试用期或正式员工可离职"),
    RESIGNATION_DATE_BEFORE_TODAY(34006, "离职日期不能早于今天"),
    // endregion

    // region 请假管理 (32xxx)
    LEAVE_BALANCE_INSUFFICIENT(62001, "假期余额不足"),
    LEAVE_NOT_FOUND(62002, "请假申请不存在"),
    LEAVE_STATUS_CANNOT_MODIFY(62003, "当前状态不允许此操作"),
    // endregion

    // region 补卡管理 (33xxx)
    SUPPLEMENT_CARD_DUPLICATE(63001, "该日期已存在补卡申请，请勿重复提交"),
    SUPPLEMENT_CARD_LIMIT_EXCEEDED(63002, "当月补卡次数已达上限（2次）"),
    // endregion

    // region 个人中心 (50xxx)
    EMPLOYEE_NOT_FOUND(50001, "员工不存在"),
    PROFILE_FIELD_NOT_EDITABLE(50002, "不允许编辑该字段"),
    OLD_PASSWORD_ERROR(50003, "旧密码错误"),
    NEW_PASSWORD_WEAK(50004, "新密码不符合复杂度要求（至少8位，含大小写字母和数字）"),
    NEW_PASSWORD_SAME_AS_OLD(50005, "新密码与旧密码相同"),
    NEW_PASSWORD_RECENTLY_USED(50006, "新密码为最近使用过的密码"),
    PHONE_ALREADY_USED(50007, "手机号已被占用"),
    VERIFY_CODE_ERROR(50008, "验证码错误或已过期"),
    VERIFY_CODE_TOO_FREQUENT(50009, "验证码发送过于频繁，请稍后再试"),
    ALREADY_CLOCKED_TODAY(50010, "今日已打卡"),
    WEB_CLOCK_DISABLED(50011, "考勤组未开启网页打卡"),
    LEAVE_NOT_BELONG_TO_USER(50012, "该请假申请不属于当前用户"),
    LEAVE_CANCEL_ONLY_PENDING(50013, "仅审批中状态可取消"),
    PAYSLIP_NOT_FOUND(50014, "工资条不存在或不属于当前用户"),
    VERIFY_CODE_LIMIT_EXCEEDED(50015, "验证码验证次数超限，请稍后再试");
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
