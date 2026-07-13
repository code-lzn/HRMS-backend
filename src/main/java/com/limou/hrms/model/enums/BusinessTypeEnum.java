package com.limou.hrms.model.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 业务类型枚举
 */
@Getter
public enum BusinessTypeEnum {

    ONBOARDING("ONBOARDING", "入职审批"),
    REGULARIZATION("REGULARIZATION", "转正审批"),
    TRANSFER("TRANSFER", "调岗审批"),
    RESIGNATION("RESIGNATION", "离职审批"),
    LEAVE("LEAVE", "请假审批"),
    PATCH_CLOCK("PATCH_CLOCK", "补卡审批"),
    SALARY_BATCH("SALARY_BATCH", "薪资批次审批");

    private final String value;

    private final String text;

    BusinessTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    public static BusinessTypeEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (BusinessTypeEnum anEnum : BusinessTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

}
