package com.limou.hrms.model.enums;

import lombok.Getter;

/**
 * 工资项目类型枚举
 */
@Getter
public enum SalaryItemTypeEnum {

    FIXED_INCOME(1, "固定收入"),
    VARIABLE_INCOME(2, "变动收入"),
    ATTENDANCE_DEDUCT(3, "考勤扣款"),
    SOCIAL_SECURITY(4, "社保扣除"),
    HOUSING_FUND(5, "公积金扣除"),
    INCOME_TAX(6, "个税");

    private final int value;
    private final String label;

    SalaryItemTypeEnum(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public static SalaryItemTypeEnum fromValue(int value) {
        for (SalaryItemTypeEnum e : values()) {
            if (e.value == value) {
                return e;
            }
        }
        return null;
    }
}
