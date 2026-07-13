package com.limou.hrms.model.enums;

/**
 * 录用类型枚举
 */
public enum EmploymentType {
    FULL_TIME("FULL_TIME", "全职"),
    PART_TIME("PART_TIME", "兼职"),
    INTERN("INTERN", "实习");

    private final String code;
    private final String desc;

    EmploymentType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }

    public static String getDesc(String code) {
        for (EmploymentType t : values()) {
            if (t.code.equals(code)) return t.desc;
        }
        return code;
    }
}
