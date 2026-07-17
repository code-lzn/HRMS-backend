package com.limou.hrms.model.enums;

import lombok.Getter;

/**
 * 离职申请状态枚举
 */
@Getter
public enum ResignationStatus {

    DRAFT(1, "草稿"),
    PENDING(2, "审批中"),
    APPROVED(3, "待离职"),
    RESIGNED(4, "已离职"),
    REJECTED(5, "已拒绝");

    private final int code;
    private final String desc;

    ResignationStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ResignationStatus fromCode(Integer code) {
        if (code == null) return null;
        for (ResignationStatus s : values()) {
            if (s.code == code) return s;
        }
        return null;
    }
}
