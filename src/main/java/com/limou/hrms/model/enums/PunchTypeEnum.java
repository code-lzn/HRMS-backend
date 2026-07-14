package com.limou.hrms.model.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 打卡方式枚举
 */
@Getter
public enum PunchTypeEnum {

    WEB(0, "网页打卡"),
    APP(1, "APP打卡");

    private final Integer value;

    private final String text;

    PunchTypeEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    public static PunchTypeEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (PunchTypeEnum anEnum : PunchTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

}
