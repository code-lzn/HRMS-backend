package com.limou.hrms.model.vo;

import java.io.Serializable;
import java.util.Map;
import lombok.Data;

/**
 * 考勤日历视图（按月份）
 */
@Data
public class AttendanceCalendarVO implements Serializable {

    /** 月份 yyyy-MM */
    private String month;

    /** 当月考勤统计：出勤天数 */
    private int normalDays;

    /** 当月考勤统计：迟到天数 */
    private int lateDays;

    /** 当月考勤统计：请假天数 */
    private int leaveDays;

    /** 当月考勤统计：缺卡天数 */
    private int missingDays;

    /** 当月考勤统计：旷工天数 */
    private int absentDays;

    /** 日期 -> 考勤状态 映射，key为'yyyy-MM-dd'，value为状态 0=正常 1=迟到 2=早退 3=缺卡 4=请假 5=旷工 */
    private Map<String, Integer> dailyStatus;

    /** 日期 -> 状态文本 映射，key为'yyyy-MM-dd'，value为状态文本（如"迟到&早退"） */
    private Map<String, String> dailyStatusText;

    /** 可补卡日期列表（缺卡的日期） */
    private java.util.List<String> makeupAvailableDates;

    private static final long serialVersionUID = 1L;
}
