package com.limou.hrms.model.enums;

import com.limou.hrms.common.ErrorCode;
import com.limou.hrms.exception.BusinessException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 职位序列枚举
 */
public enum PositionSequence {

    MANAGEMENT(1, "M", "管理序列", 5),
    PROFESSIONAL(2, "P", "专业序列", 10),
    SUPPORT(3, "S", "支持序列", 5);

    private final int value;
    private final String prefix;
    private final String desc;
    private final int maxLevel;

    PositionSequence(int value, String prefix, String desc, int maxLevel) {
        this.value = value;
        this.prefix = prefix;
        this.desc = desc;
        this.maxLevel = maxLevel;
    }

    /**
     * 校验职级范围是否合法
     *
     * @param levelMin 职级下限
     * @param levelMax 职级上限
     */
    public void validateLevelRange(int levelMin, int levelMax) {
        if (levelMin > levelMax) {
            throw new BusinessException(ErrorCode.POSITION_LEVEL_RANGE_INVALID, "职级下限不能大于上限");
        }
        if (levelMin < 1 || levelMax > this.maxLevel) {
            throw new BusinessException(ErrorCode.POSITION_LEVEL_RANGE_INVALID,
                    String.format("%s序列的职级范围必须在1-%d之间", this.desc, this.maxLevel));
        }
    }

    /**
     * 获取职级范围字符串（如 P1-P10）
     */
    public String getLevelRange(int levelMin, int levelMax) {
        return this.prefix + levelMin + "-" + this.prefix + levelMax;
    }

    /**
     * 根据 value 获取枚举
     */
    public static PositionSequence getByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (PositionSequence seq : values()) {
            if (seq.value == value) {
                return seq;
            }
        }
        return null;
    }

    /**
     * 获取所有序列的典型职位示例
     */
    public List<String> getTypicalPositions() {
        switch (this) {
            case MANAGEMENT:
                return Arrays.asList("M1主管", "M2经理", "M3总监", "M5VP");
            case PROFESSIONAL:
                return Arrays.asList("P3初级", "P5中级", "P7高级", "P9专家");
            case SUPPORT:
                return Arrays.asList("S1职能", "S2职能", "S3职能", "S4职能");
            default:
                return Collections.emptyList();
        }
    }

    public int getValue() {
        return value;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getDesc() {
        return desc;
    }

    public int getMaxLevel() {
        return maxLevel;
    }
}
