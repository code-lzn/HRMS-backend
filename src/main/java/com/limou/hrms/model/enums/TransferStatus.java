package com.limou.hrms.model.enums;

import lombok.Getter;

/**
 * 调岗申请状态枚举
 */
@Getter
public enum TransferStatus {

    DRAFT(1, "草稿"),
    PENDING(2, "审批中"),
    EFFECTIVE(3, "已生效"),
    REJECTED(4, "已拒绝");

    private final int code;
    private final String desc;

    TransferStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static TransferStatus fromCode(Integer code) {
        if (code == null) return null;
        for (TransferStatus s : values()) {
            if (s.code == code) return s;
        }
        return null;
    }
}
