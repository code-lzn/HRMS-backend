package com.limou.hrms.model.enums;

/**
 * 合同类型枚举
 */
public enum ContractType {
    FIXED(1, "固定期限"),
    INDEFINITE(2, "无固定期限"),
    LABOR(3, "劳务合同");

    private final int code;
    private final String desc;

    ContractType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }

    public static String getDesc(int code) {
        for (ContractType t : values()) {
            if (t.code == code) return t.desc;
        }
        return "未知";
    }
}
