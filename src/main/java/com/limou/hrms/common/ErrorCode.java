package com.limou.hrms.common;

/**
 * 自定义错误码
 *
 */
public enum ErrorCode {

    SUCCESS(0, "ok"),
    PARAMS_ERROR(40000, "请求参数错误"),
    NOT_LOGIN_ERROR(40100, "未登录"),
    NO_AUTH_ERROR(40101, "无权限"),
    NOT_FOUND_ERROR(40400, "请求数据不存在"),
    FORBIDDEN_ERROR(40300, "禁止访问"),
    SYSTEM_ERROR(50000, "系统内部异常"),
    OPERATION_ERROR(50001, "操作失败"),
    ATTENDANCE_DUPLICATE_ERROR(50010, "今日已打卡，请勿重复打卡"),
    ATTENDANCE_TIME_ERROR(50011, "不在打卡时间范围内"),
    LEAVE_OVERLAP_ERROR(50012, "请假时间与已有请假记录重叠"),
    LEAVE_DAYS_ERROR(50013, "请假天数不足"),
    APPROVAL_NOT_PENDING_ERROR(50014, "该申请已审批，无法重复操作"),
    SALARY_VERIFY_FAILED(50015, "密码验证失败"),
    SALARY_VERIFY_LOCKED(50016, "验证次数过多，请30分钟后重试"),
    SALARY_NOT_FOUND(50017, "工资条不存在"),
    PASSWORD_ERROR(50018, "原密码错误"),
    PASSWORD_SAME_AS_OLD(50019, "新密码不能与旧密码相同"),
    PASSWORD_RECENTLY_USED(50020, "新密码与近期使用过的密码重复"),
    PHONE_ALREADY_BOUND(50021, "该手机号已被其他账号绑定"),
    APPROVAL_NOT_FOUND(50022, "审批记录不存在"),
    APPROVAL_NOT_PENDING(50023, "该节点已处理，无法重复操作"),
    APPROVAL_NO_PERMISSION(50024, "无审批权限"),
    DELEGATION_NOT_FOUND(50025, "委托记录不存在"),
    EMPLOYEE_RESIGNED(50026, "已离职员工不可修改档案，仅可查看"),
    PROFILE_PERMISSION_DENIED(50027, "无权查看他人档案，仅可查看本人"),
    PROFILE_UPDATE_DENIED(50028, "无权修改他人档案，仅可修改本人"),
    PHONE_FORMAT_ERROR(50029, "紧急联系人电话格式不正确，需为11位手机号");

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

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}
