package com.limou.hrms.model.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 工资项目类型枚举
 */
public enum SalaryItemTypeEnum {

    FIXED_INCOME(1, "固定收入"),
    VARIABLE_INCOME(2, "变动收入"),
    ATTENDANCE_DEDUCT(3, "考勤扣款"),
    SOCIAL_SECURITY(4, "社保扣除"),
    HOUSING_FUND(5, "公积金扣除"),
    INCOME_TAX(6, "个人所得税");

    private final int value;
    private final String label;

    SalaryItemTypeEnum(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    public static SalaryItemTypeEnum fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (SalaryItemTypeEnum e : SalaryItemTypeEnum.values()) {
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
