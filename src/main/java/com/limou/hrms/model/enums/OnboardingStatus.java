package com.limou.hrms.model.enums;

import lombok.Getter;

/**
 * 入职申请状态枚举
 */
@Getter
public enum OnboardingStatus {

    DRAFT(1, "草稿"),
    PENDING(2, "审批中"),
    APPROVED(3, "已批准待入职"),
    JOINED(4, "已入职"),
    REJECTED(5, "已拒绝"),
    ABANDONED(6, "已放弃");

    private final int code;
    private final String desc;

    OnboardingStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static OnboardingStatus fromCode(Integer code) {
        if (code == null) return null;
        for (OnboardingStatus s : values()) {
            if (s.code == code) return s;
        }
        return null;
    }
}
