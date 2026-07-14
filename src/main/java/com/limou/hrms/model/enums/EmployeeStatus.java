package com.limou.hrms.model.enums;

/**
 * 员工在职状态枚举
 */
public enum EmployeeStatus {
    PROBATION(1, "试用期"),
    REGULAR(2, "正式"),
    PENDING_LEAVE(3, "待离职"),
    RESIGNED(4, "已离职");

    private final int code;
    private final String desc;

    EmployeeStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }

    public static String getDesc(int code) {
        for (EmployeeStatus s : values()) {
            if (s.code == code) return s.desc;
        }
        return "未知";
    }
}
