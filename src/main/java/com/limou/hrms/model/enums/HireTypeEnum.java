package com.limou.hrms.model.enums;

import lombok.Getter;

/**
 * 录用类型枚举
 */
@Getter
public enum HireTypeEnum {

    FULL_TIME(1, "全职"),
    PART_TIME(2, "兼职"),
    INTERNSHIP(3, "实习");

    private final int value;
    private final String desc;

    HireTypeEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static HireTypeEnum getByValue(int value) {
        for (HireTypeEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
