package com.limou.hrms.model.enums;

import lombok.Getter;

/**
 * 入职申请状态枚举
 */
@Getter
public enum OnboardingStatusEnum {

    DRAFT(0, "草稿"),
    PENDING(1, "审批中"),
    APPROVED(2, "已批准待入职"),
    REJECTED(3, "已拒绝"),
    ONBOARDED(4, "已入职"),
    ABANDONED(5, "已放弃");

    private final int value;
    private final String desc;

    OnboardingStatusEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static OnboardingStatusEnum getByValue(int value) {
        for (OnboardingStatusEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
