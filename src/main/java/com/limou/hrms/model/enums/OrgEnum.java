package com.limou.hrms.model.enums;

/**
 * 组织架构模块常量
 */
public enum OrgEnum {

    MAX_DEPT_DEPTH(5, "最大部门层级深度");

    private final int code;
    private final String desc;

    OrgEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }

    public static String getDesc(int code) {
        for (OrgEnum t : values()) {
            if (t.code == code) return t.desc;
        }
        return "未知";
    }
}
