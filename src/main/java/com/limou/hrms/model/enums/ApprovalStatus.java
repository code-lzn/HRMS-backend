package com.limou.hrms.model.enums;

/**
 * 审批实例状态枚举
 */
public enum ApprovalStatus {

    PENDING(1, "审批中"),
    APPROVED(2, "已通过"),
    REJECTED(3, "已拒绝"),
    CANCELLED(4, "已撤回");

    private final int code;
    private final String desc;

    ApprovalStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }

    public static ApprovalStatus fromCode(int code) {
        for (ApprovalStatus s : values()) {
            if (s.code == code) { return s; }
        }
        return null;
    }
}
