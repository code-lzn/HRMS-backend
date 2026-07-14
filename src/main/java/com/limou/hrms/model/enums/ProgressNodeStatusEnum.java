package com.limou.hrms.model.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 审批进度节点状态枚举
 */
@Getter
public enum ProgressNodeStatusEnum {

    COMPLETED(0, "已完成"),
    IN_PROGRESS(1, "进行中"),
    NOT_STARTED(2, "未开始");

    private final Integer value;

    private final String text;

    ProgressNodeStatusEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    public static ProgressNodeStatusEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (ProgressNodeStatusEnum anEnum : ProgressNodeStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

}
