package com.limou.hrms.model.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 审批操作枚举
 */
@Getter
public enum ApprovalActionEnum {

    PENDING("PENDING", "待审批"),
    APPROVE("APPROVE", "通过"),
    REJECT("REJECT", "拒绝"),
    TRANSFER("TRANSFER", "转交");

    private final String value;

    private final String text;

    ApprovalActionEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    public static ApprovalActionEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (ApprovalActionEnum anEnum : ApprovalActionEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
