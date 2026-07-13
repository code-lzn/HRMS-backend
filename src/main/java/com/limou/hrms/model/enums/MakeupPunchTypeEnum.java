package com.limou.hrms.model.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 补卡类型枚举
 */
@Getter
public enum MakeupPunchTypeEnum {

    PUNCH_IN(0, "上班补卡"),
    PUNCH_OUT(1, "下班补卡");

    private final Integer value;

    private final String text;

    MakeupPunchTypeEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    public static MakeupPunchTypeEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (MakeupPunchTypeEnum anEnum : MakeupPunchTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

}
