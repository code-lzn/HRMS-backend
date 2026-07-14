package com.limou.hrms.model.enums;

import lombok.Getter;

@Getter
public enum DataScopeEnum {

    ALL(1, "全量"),
    ALL_EMPLOYEE(2, "全部员工"),
    DEPARTMENT(3, "本部门及下属"),
    SALARY(4, "薪资相关"),
    SELF(5, "仅本人");

    private final Integer code;
    private final String desc;

    DataScopeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static DataScopeEnum getByCode(Integer code) {
        if (code == null) {
            return SELF;
        }
        for (DataScopeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return SELF;
    }

    public static String getDescByCode(Integer code) {
        return getByCode(code).getDesc();
    }
}