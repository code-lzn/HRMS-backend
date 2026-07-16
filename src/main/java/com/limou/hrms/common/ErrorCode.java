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
    POSITION_NAME_DUPLICATE(30013, "同一部门下职位名称重复");
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
