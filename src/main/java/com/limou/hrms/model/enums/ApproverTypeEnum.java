package com.limou.hrms.model.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 审批人类型枚举
 */
@Getter
public enum ApproverTypeEnum {

    DEPT_MANAGER("DEPT_MANAGER", "部门负责人"),
    DIRECT_SUPERIOR("DIRECT_SUPERIOR", "直接上级"),
    HR_MANAGER("HR_MANAGER", "HR负责人"),
    FINANCE("FINANCE", "财务专员"),
    BOSS("BOSS", "老板"),
    SPECIFIED("SPECIFIED", "指定人");

    private final String value;

    private final String text;

    ApproverTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    public static ApproverTypeEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (ApproverTypeEnum anEnum : ApproverTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

}
