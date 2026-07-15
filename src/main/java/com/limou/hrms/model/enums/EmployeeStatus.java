package com.limou.hrms.model.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 员工在职状态枚举
 */
@Getter
public enum EmployeeStatus {

    PROBATION(1, "试用期"),
    REGULAR(2, "正式"),
    PENDING_RESIGNATION(3, "待离职"),
    RESIGNED(4, "已离职");

    private final int value;
    private final String desc;

    EmployeeStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    /**
     * 是否为在职状态（用于人数统计、部门删除校验等）
     */
    public boolean isActive() {
        return this == PROBATION || this == REGULAR;
    }

    /**
     * 获取所有在职状态值列表（用于 SQL IN 条件）
     */
    public static List<Integer> getActiveValues() {
        return Arrays.asList(PROBATION.value, REGULAR.value);
    }

    /**
     * 根据 value 获取枚举
     */
    public static EmployeeStatus getByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (EmployeeStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        return null;
    }

}
