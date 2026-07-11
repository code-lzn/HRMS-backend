package com.limou.hrms.model.enums;

import lombok.Getter;

/**
 * 离职申请状态枚举
 */
@Getter
public enum ResignationStatusEnum {

    PENDING(1, "审批中"),
    APPROVED_PENDING_LEAVE(2, "已通过待离职"),
    REJECTED(3, "已拒绝"),
    RESIGNED(4, "已离职");

    private final int value;
    private final String desc;

    ResignationStatusEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static ResignationStatusEnum getByValue(int value) {
        for (ResignationStatusEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }
}
