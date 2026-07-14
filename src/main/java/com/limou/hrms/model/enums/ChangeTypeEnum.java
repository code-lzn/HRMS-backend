package com.limou.hrms.model.enums;

import lombok.Getter;

/**
 * 调薪变更类型枚举
 */
@Getter
public enum ChangeTypeEnum {

    SALARY_ADJUST(1, "调薪"),
    ACCOUNT_CHANGE(2, "账套变更"),
    BASE_ADJUST(3, "基数调整"),
    REGULARIZATION(4, "转正调薪"),
    TRANSFER(5, "调岗调薪");

    private final int value;
    private final String text;

    ChangeTypeEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public static ChangeTypeEnum getByValue(Integer value) {
        if (value == null) return null;
        for (ChangeTypeEnum e : values()) {
            if (e.value == value) return e;
        }
        return null;
    }
}
