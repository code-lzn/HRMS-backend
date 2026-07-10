package com.limou.hrms.model.enums;

import lombok.Getter;

/**
 * 适用范围类型枚举
 */
@Getter
public enum ScopeTypeEnum {

    DEPARTMENT(1, "部门"),
    POSITION(2, "职位"),
    LEVEL(3, "职级");

    private final int value;
    private final String label;

    ScopeTypeEnum(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public static ScopeTypeEnum fromValue(int value) {
        for (ScopeTypeEnum e : values()) {
            if (e.value == value) {
                return e;
            }
        }
        return null;
    }
}
