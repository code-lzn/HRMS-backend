package com.limou.hrms.model.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 薪资核算批次状态枚举
 */
public enum BatchStatusEnum {

    DRAFT(0, "草稿"),
    CALCULATING(1, "计算中"),
    CONFIRMING(2, "待确认"),
    APPROVING(3, "审批中"),
    APPROVED(4, "已通过"),
    PAID(5, "已发放"),
    REJECTED(6, "已驳回");

    private final int value;
    private final String label;

    BatchStatusEnum(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    public static BatchStatusEnum fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (BatchStatusEnum e : BatchStatusEnum.values()) {
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
