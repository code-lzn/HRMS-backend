package com.limou.hrms.model.enums;

/**
 * 审批业务类型枚举
 */
public enum ApprovalBizType {

    ONBOARDING("ONBOARDING", "入职审批"),
    PROBATION("PROBATION", "转正审批");

    private final String code;
    private final String desc;

    ApprovalBizType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }

    public static ApprovalBizType fromCode(String code) {
        for (ApprovalBizType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
