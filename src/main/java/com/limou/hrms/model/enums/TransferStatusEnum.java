package com.limou.hrms.model.enums;

import lombok.Getter;

/**
 * 调岗审批状态枚举
 */
@Getter
public enum TransferStatusEnum {

    PENDING_OLD_DEPT(1, "原部门审批中"),
    PENDING_NEW_DEPT(2, "新部门审批中"),
    PENDING_HR(3, "HR备案中"),
    APPROVED(4, "已通过"),
    REJECTED(5, "已拒绝");

    private final int value;
    private final String desc;

    TransferStatusEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static TransferStatusEnum getByValue(int value) {
        for (TransferStatusEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
