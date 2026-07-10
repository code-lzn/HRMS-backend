package com.limou.hrms.constant;

/**
 * 组织架构模块常量
 */
public interface OrgConstant {

    /** 部门最大层级深度 */
    int MAX_DEPT_DEPTH = 5;

    /** 部门编码长度 */
    int DEPT_CODE_LENGTH = 2;

    /** 职位序列：管理序列 */
    int SEQUENCE_MANAGEMENT = 1;

    /** 职位序列：专业序列 */
    int SEQUENCE_PROFESSIONAL = 2;

    /** 职位序列：支持序列 */
    int SEQUENCE_SUPPORT = 3;

    /** 员工状态：试用期 */
    int EMPLOYEE_STATUS_PROBATION = 1;

    /** 员工状态：正式 */
    int EMPLOYEE_STATUS_REGULAR = 2;

    /** 员工状态：离职 */
    int EMPLOYEE_STATUS_RESIGNED = 3;

    /**
     * 职位序列枚举
     */
    enum PositionSequence {
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

        /**
         * 判断职级是否在序列允许范围内
         */
        public boolean inRange(String level) {
            if (level == null || !level.startsWith(prefix)) return false;
            for (String l : levels) {
                if (l.equals(level)) return true;
            }
            return false;
        }
    }
}
