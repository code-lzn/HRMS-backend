package com.limou.hrms.model.enums;

/**
 * 审批节点状态枚举
 */
public enum NodeStatus {

    PENDING(1, "待审批"),
    APPROVED(2, "已通过"),
    REJECTED(3, "已拒绝"),
    TRANSFERRED(4, "已转交"),
    TIMEOUT(5, "已超时");

    private final int code;
    private final String desc;

    NodeStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }

    public static NodeStatus fromCode(int code) {
        for (NodeStatus s : values()) {
            if (s.code == code) { return s; }
        }
        return null;
    }
}
