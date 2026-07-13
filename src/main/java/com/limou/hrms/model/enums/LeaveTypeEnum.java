package com.limou.hrms.model.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 请假类型枚举
 */
@Getter
public enum LeaveTypeEnum {

    PERSONAL(0, "事假"),
    SICK(1, "病假"),
    ANNUAL(2, "年假"),
    MARRIAGE(3, "婚假"),
    MATERNITY(4, "产假"),
    FUNERAL(5, "丧假"),
    COMPENSATORY(6, "调休");

    private final Integer value;

    private final String text;

    LeaveTypeEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 获取值列表
     *
     * @return
     */
    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value
     * @return
     */
    public static LeaveTypeEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (LeaveTypeEnum anEnum : LeaveTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

}
