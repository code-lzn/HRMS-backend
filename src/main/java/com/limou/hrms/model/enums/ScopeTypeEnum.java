package com.limou.hrms.model.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 账套适用范围类型枚举
 */
public enum ScopeTypeEnum {

    DEPT(1, "部门"),
    POSITION(2, "职位"),
    LEVEL(3, "职级");

    private final int value;
    private final String label;

    ScopeTypeEnum(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    public static ScopeTypeEnum fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (ScopeTypeEnum e : ScopeTypeEnum.values()) {
            if (e.value == value) {
                return e;
            }
        }
        return null;
    }

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }
}
