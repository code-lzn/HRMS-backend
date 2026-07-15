package com.limou.hrms.model.enums;

import lombok.Getter;

/**
 * 账套适用范围类型枚举
 */
@Getter
public enum ScopeTypeEnum {

    DEPARTMENT(1, "部门"),
    POSITION(2, "职位"),
    JOB_LEVEL(3, "职级");

    private final int value;
    private final String text;

    ScopeTypeEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public static ScopeTypeEnum getByValue(Integer value) {
        if (value == null) return null;
        for (ScopeTypeEnum e : values()) {
            if (e.value == value) return e;
        }
        return null;
    }
}
