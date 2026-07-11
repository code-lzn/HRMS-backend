package com.limou.hrms.model.enums;

import lombok.Getter;

/**
 * 转正审批结果枚举
 */
@Getter
public enum RegularizationResultEnum {

    PASS(1, "通过"),
    EXTEND(2, "延长试用"),
    FAIL(3, "不通过");

    private final int value;
    private final String desc;

    RegularizationResultEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static RegularizationResultEnum getByValue(int value) {
        for (RegularizationResultEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
