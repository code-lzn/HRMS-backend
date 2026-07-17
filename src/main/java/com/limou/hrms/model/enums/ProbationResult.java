package com.limou.hrms.model.enums;

import lombok.Getter;

/**
 * 转正结果枚举
 */
@Getter
public enum ProbationResult {

    PASS(1, "通过转正"),
    EXTEND(2, "延长试用"),
    FAIL(3, "不通过");

    private final int code;
    private final String desc;

    ProbationResult(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ProbationResult fromCode(Integer code) {
        if (code == null) return null;
        for (ProbationResult r : values()) {
            if (r.code == code) return r;
        }
        return null;
    }
}
