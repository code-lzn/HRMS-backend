package com.limou.hrms.model.enums;

/**
 * 审批业务类型枚举
 */
public enum ApprovalBizType {

    ONBOARDING("ONBOARDING", "入职审批"),
    PROBATION("PROBATION", "转正审批"),
    TRANSFER("TRANSFER", "调岗审批"),
    RESIGNATION("RESIGNATION", "离职审批"),
    LEAVE("LEAVE", "请假审批"),
    CARD_REPLENISH("CARD_REPLENISH", "补卡审批"),
    SALARY_BATCH("SALARY_BATCH", "薪资批次审批");

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
