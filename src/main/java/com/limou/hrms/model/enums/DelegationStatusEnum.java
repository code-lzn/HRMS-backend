package com.limou.hrms.model.enums;

import lombok.Getter;

/**
 * 委托审批状态枚举
 */
@Getter
public enum DelegationStatusEnum {

    CANCELLED(0, "已取消"),
    ACTIVE(1, "有效");

    private final Integer value;
    private final String text;

    DelegationStatusEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    public static DelegationStatusEnum getEnumByValue(Integer value) {
        if (value == null) return null;
        for (DelegationStatusEnum e : values()) {
            if (e.value.equals(value)) return e;
        }
        return null;
    }
}
