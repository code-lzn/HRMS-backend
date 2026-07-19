package com.limou.hrms.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 请假申请状态枚举
 */
@Getter
@AllArgsConstructor
public enum LeaveStatus {

    DRAFT(1, "草稿"),
    PENDING(2, "审批中"),
    APPROVED(3, "已通过"),
    REJECTED(4, "已拒绝"),
    CANCELLED(5, "已取消");

    private final int code;
    private final String desc;

    public static LeaveStatus fromCode(Integer code) {
        if (code == null) return null;
        for (LeaveStatus s : values()) {
            if (s.code == code) return s;
        }
        return null;
    }
}
