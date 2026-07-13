package com.limou.hrms.constant;

/**
 * 组织架构模块常量
 */
public interface OrgConstant {

    /** 部门最大层级深度 */
    int MAX_DEPT_DEPTH = 5;
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
    /** 员工状态：待离职 */
    int EMPLOYEE_STATUS_PENDING_LEAVE = 3;
    /** 员工状态：已离职 */
    int EMPLOYEE_STATUS_RESIGNED = 4;
}
