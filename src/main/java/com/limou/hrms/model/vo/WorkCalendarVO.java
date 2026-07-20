package com.limou.hrms.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 工作日历月视图 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkCalendarVO implements Serializable {

    private Integer year;

    private Integer month;

    private List<DayItem> days;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayItem implements Serializable {
        private LocalDate date;
        private Integer dayType;
        private String dayTypeDesc;
        private String holidayName;
        private static final long serialVersionUID = 1L;
    }

    private static final long serialVersionUID = 1L;
}
