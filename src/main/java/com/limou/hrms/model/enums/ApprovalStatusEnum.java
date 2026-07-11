package com.limou.hrms.model.enums;

import lombok.Getter;

/**
 * 通用审批状态枚举
 */
@Getter
public enum ApprovalStatusEnum {

    PENDING(1, "审批中"),
    APPROVED(2, "已通过"),
    REJECTED(3, "已拒绝");

    private final int value;
    private final String desc;

    ApprovalStatusEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static ApprovalStatusEnum getByValue(int value) {
        for (ApprovalStatusEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
