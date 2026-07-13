package com.limou.hrms.model.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 审批记录状态枚举
 */
@Getter
public enum ApprovalRecordStatusEnum {

    APPROVING("APPROVING", "审批中"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "已拒绝"),
    WITHDRAWN("WITHDRAWN", "已撤回");

    private final String value;

    private final String text;

    ApprovalRecordStatusEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    public static ApprovalRecordStatusEnum getEnumByValue(String value) {
        if (value == null) {
            return null;
        }
        for (ApprovalRecordStatusEnum anEnum : ApprovalRecordStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

}
