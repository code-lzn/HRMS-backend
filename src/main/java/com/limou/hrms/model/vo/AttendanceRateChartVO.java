package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class AttendanceRateChartVO implements Serializable {

    private List<String> months;

    private List<SeriesItem> series;

    @Data
    public static class SeriesItem implements Serializable {
        private String departmentName;
        private List<BigDecimal> rates;
        private static final long serialVersionUID = 1L;
    }

    private static final long serialVersionUID = 1L;
}