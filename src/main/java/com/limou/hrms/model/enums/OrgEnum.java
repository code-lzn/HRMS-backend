package com.limou.hrms.model.enums;

/**
 * 组织架构模块常量
 */
public enum OrgEnum {

    MAX_DEPT_DEPTH(5);

    private final int value;

    OrgEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
