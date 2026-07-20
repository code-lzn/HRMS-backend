package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 考勤日历视图 VO
 */
@Data
public class AttendanceCalendarVO implements Serializable {

    private int year;

    private int month;

    private List<DayItem> days;

    private Summary summary;

    @Data
    public static class DayItem implements Serializable {
        private String date;
        private Integer dayType;
        private String dayTypeDesc;
        private Integer startStatus;
        private String startStatusDesc;
        private Integer endStatus;
        private String endStatusDesc;
        private Boolean hasLeave;
        /** 上班打卡时间 HH:mm */
        private String clockIn;
        /** 下班打卡时间 HH:mm */
        private String clockOut;
        private static final long serialVersionUID = 1L;
    }

    @Data
    public static class Summary implements Serializable {
        private int normalDays;
        private int lateDays;
        private int earlyLeaveDays;
        private int absentDays;
        private int cardMissingDays;
        private int leaveDays;
        private static final long serialVersionUID = 1L;
    }

    private static final long serialVersionUID = 1L;
}
