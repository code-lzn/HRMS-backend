package com.limou.hrms.constant;

/**
 * 考勤模块常量
 */
public interface AttendanceConstant {

    // ==================== 考勤状态 ====================
    /** 正常 */
    int ATTENDANCE_STATUS_NORMAL = 0;
    /** 迟到 */
    int ATTENDANCE_STATUS_LATE = 1;
    /** 早退 */
    int ATTENDANCE_STATUS_LEAVE_EARLY = 2;
    /** 缺卡 */
    int ATTENDANCE_STATUS_MISSING = 3;
    /** 请假 */
    int ATTENDANCE_STATUS_LEAVE = 4;
    /** 旷工 */
    int ATTENDANCE_STATUS_ABSENT = 5;

    // ==================== 打卡方式 ====================
    /** 网页打卡 */
    int PUNCH_TYPE_WEB = 0;
    /** APP打卡 */
    int PUNCH_TYPE_APP = 1;

    // ==================== 请假类型 ====================
    /** 事假 */
    int LEAVE_TYPE_PERSONAL = 0;
    /** 病假 */
    int LEAVE_TYPE_SICK = 1;
    /** 年假 */
    int LEAVE_TYPE_ANNUAL = 2;
    /** 婚假 */
    int LEAVE_TYPE_MARRIAGE = 3;
    /** 产假 */
    int LEAVE_TYPE_MATERNITY = 4;
    /** 丧假 */
    int LEAVE_TYPE_FUNERAL = 5;
    /** 调休 */
    int LEAVE_TYPE_COMPENSATORY = 6;

    // ==================== 审批状态 ====================
    /** 待审批 */
    int APPROVAL_STATUS_PENDING = 0;
    /** 已通过 */
    int APPROVAL_STATUS_APPROVED = 1;
    /** 已拒绝 */
    int APPROVAL_STATUS_REJECTED = 2;
    /** 已撤销（仅请假） */
    int APPROVAL_STATUS_CANCELLED = 3;

    // ==================== 补卡类型 ====================
    /** 上班补卡 */
    int MAKEUP_TYPE_PUNCH_IN = 0;
    /** 下班补卡 */
    int MAKEUP_TYPE_PUNCH_OUT = 1;

    // ==================== 系统配置 ====================
    /** 默认上班时间（时） */
    int DEFAULT_WORK_START_HOUR = 9;
    /** 默认下班时间（时） */
    int DEFAULT_WORK_END_HOUR = 18;
    /** 迟到宽限分钟数 */
    int LATE_GRACE_MINUTES = 5;
}
