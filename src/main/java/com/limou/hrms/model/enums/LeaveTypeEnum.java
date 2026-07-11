package com.limou.hrms.model.enums;

import lombok.Getter;

/**
 * 离职类型枚举
 */
@Getter
public enum LeaveTypeEnum {

    RESIGN(1, "辞职"),
    DISMISS(2, "辞退"),
    CONTRACT_EXPIRE(3, "合同到期不续签"),
    OTHER(4, "其他");

    private final int value;
    private final String desc;

    LeaveTypeEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static LeaveTypeEnum getByValue(int value) {
        for (LeaveTypeEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
