package com.limou.hrms.constant;

/**
 * 数据权限范围枚举
 */
public enum DataScopeEnum {

    /** 全部数据 — admin 看全公司 */
    ALL,

    /** 本部门及下属部门 — dept_head 看管辖范围 */
    DEPT,

    /** 仅自己 — 普通员工看个人数据 */
    SELF,

    /** 无权访问 */
    NONE;

    /**
     * 是否可查看全量
     */
    public boolean canViewAll() {
        return this == ALL;
    }

    /**
     * 是否可查看部门范围
     */
    public boolean canViewDept() {
        return this == ALL || this == DEPT;
    }

    /**
     * 是否可查看自己
     */
    public boolean canViewSelf() {
        return this != NONE;
    }
}
