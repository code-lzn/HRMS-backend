package com.limou.hrms.model.enums;

import lombok.Getter;

/**
 * 异常等级枚举
 */
@Getter
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

    public static AbnormalLevelEnum fromValue(int value) {
        for (AbnormalLevelEnum e : values()) {
            if (e.value == value) {
                return e;
            }
        }
        return null;
    }
}
