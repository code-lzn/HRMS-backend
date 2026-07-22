package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 考勤日历视图 VO
 */
@Data
public class AttendanceCalendarVO implements Serializable {

    /** 年份 */
    private int year;

    /** 月份 */
    private int month;

    /** 每日数据列表 */
    private List<DayItem> days;

    /** 月度汇总统计 */
    private Summary summary;

    /** 每日详情 */
    @Data
    public static class DayItem implements Serializable {
        /** 日期 */
        private String date;
        /** 日期类型：1=工作日 2=休息日 3=节假日 */
        private Integer dayType;
        /** 日期类型描述 */
        private String dayTypeDesc;
        /** 上班状态码：1=正常 2=迟到 3=旷工半天 4=缺卡 */
        private Integer startStatus;
        /** 上班状态描述 */
        private String startStatusDesc;
        /** 下班状态码：1=正常 2=早退 3=旷工半天 4=缺卡 */
        private Integer endStatus;
        /** 下班状态描述 */
        private String endStatusDesc;
        /** 是否请假 */
        private Boolean hasLeave;
        /** 上班打卡时间 HH:mm */
        private String clockIn;
        /** 下班打卡时间 HH:mm */
        private String clockOut;
        private static final long serialVersionUID = 1L;
    }

    /** 月度统计 */
    @Data
    public static class Summary implements Serializable {
        /** 正常出勤天数 */
        private int normalDays;
        /** 迟到天数 */
        private int lateDays;
        /** 早退天数 */
        private int earlyLeaveDays;
        /** 旷工天数 */
        private int absentDays;
        /** 缺卡天数 */
        private int cardMissingDays;
        /** 请假天数 */
        private int leaveDays;
        private static final long serialVersionUID = 1L;
    }

    private static final long serialVersionUID = 1L;
}