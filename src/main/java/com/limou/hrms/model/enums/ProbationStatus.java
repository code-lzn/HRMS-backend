package com.limou.hrms.model.enums;

import lombok.Getter;

/**
 * 转正申请状态枚举
 */
@Getter
public enum ProbationStatus {

    DRAFT(1, "草稿"),
    PENDING(2, "审批中"),
    COMPLETED(3, "已完成"),
    REJECTED(4, "已拒绝");

    private final int code;
    private final String desc;

    ProbationStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ProbationStatus fromCode(Integer code) {
        if (code == null) return null;
        for (ProbationStatus s : values()) {
            if (s.code == code) return s;
        }
        return null;
    }
}
