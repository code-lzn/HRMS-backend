package com.limou.hrms.constant;

/**
 * 员工档案模块常量
 */
public interface EmployeeConstant {

    /** 在职状态：试用期 */
    int STATUS_PROBATION = 1;
    /** 在职状态：正式 */
    int STATUS_REGULAR = 2;
    /** 在职状态：待离职 */
    int STATUS_PENDING_LEAVE = 3;
    /** 在职状态：已离职 */
    int STATUS_RESIGNED = 4;

    /** 性别：女 */
    int GENDER_FEMALE = 0;
    /** 性别：男 */
    int GENDER_MALE = 1;

    /** 合同类型：固定期限 */
    int CONTRACT_FIXED = 1;
    /** 合同类型：无固定期限 */
    int CONTRACT_INDEFINITE = 2;
    /** 合同类型：劳务合同 */
    int CONTRACT_LABOR = 3;

    /** 变更类型：直接编辑 */
    String CHANGE_DIRECT_EDIT = "DIRECT_EDIT";
    /** 变更类型：流程变更 */
    String CHANGE_FLOW_CHANGE = "FLOW_CHANGE";
    /** 变更类型：系统自动 */
    String CHANGE_SYSTEM = "SYSTEM";
}
