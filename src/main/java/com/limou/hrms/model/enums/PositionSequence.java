package com.limou.hrms.model.enums;

/**
 * 职位序列枚举
 */
public enum PositionSequence {
    MANAGEMENT(1, "M", "管理序列", new String[]{"M1", "M2", "M3", "M4", "M5"}),
    PROFESSIONAL(2, "P", "专业序列", new String[]{"P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8", "P9", "P10"}),
    SUPPORT(3, "S", "支持序列", new String[]{"S1", "S2", "S3", "S4", "S5"});

    private final int code;
    private final String prefix;
    private final String name;
    private final String[] levels;

    PositionSequence(int code, String prefix, String name, String[] levels) {
        this.code = code;
        this.prefix = prefix;
        this.name = name;
        this.levels = levels;
    }

    public int getCode() { return code; }
    public String getPrefix() { return prefix; }
    public String getName() { return name; }
    public String[] getLevels() { return levels; }

    public static PositionSequence fromCode(int code) {
        for (PositionSequence seq : values()) {
            if (seq.code == code) return seq;
        }
        return null;
    }

    public boolean inRange(String level) {
        if (level == null || !level.startsWith(prefix)) return false;
        for (String l : levels) {
            if (l.equals(level)) return true;
        }
        return false;
    }
}
