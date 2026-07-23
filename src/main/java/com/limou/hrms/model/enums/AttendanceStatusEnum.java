package com.limou.hrms.model.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 考勤状态枚举
 */
@Getter
public enum AttendanceStatusEnum {

    NORMAL(0, "正常"),
    LATE(1, "迟到"),
    LEAVE_EARLY(2, "早退"),
    MISSING(3, "缺卡"),
    LEAVE(4, "请假"),
    ABSENT(5, "旷工"),
    MISS_IN(6, "上班缺卡"),
    MISS_OUT(7, "下班缺卡"),
    REST(8, "休息"),
    LATE_AND_EARLY(9, "迟到&早退"),
    SEVERE_LATE(10, "严重迟到");

    private final Integer value;

    private final String text;

    AttendanceStatusEnum(Integer value, String text) {
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
    public static AttendanceStatusEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (AttendanceStatusEnum anEnum : AttendanceStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

}
