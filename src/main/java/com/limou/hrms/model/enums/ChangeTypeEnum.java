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
    REGULAR_ADJUST(4, "转正调薪"),
    TRANSFER_ADJUST(5, "调岗调薪");

    private final int value;
    private final String label;

    ChangeTypeEnum(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public static ChangeTypeEnum fromValue(int value) {
        for (ChangeTypeEnum e : values()) {
            if (e.value == value) {
                return e;
            }
        }
        return null;
    }
}
