package com.limou.hrms.model.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 薪资异常级别枚举
 */
public enum AbnormalLevelEnum {

    NORMAL(0, "正常"),
    WARNING(1, "黄色预警"),
    ERROR(2, "红色预警"),
    BLOCKED(3, "红色阻断");

    private final int value;
    private final String label;

    AbnormalLevelEnum(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    public static AbnormalLevelEnum fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (AbnormalLevelEnum e : AbnormalLevelEnum.values()) {
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
