package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 部门出勤率趋势图表 VO（折线图）
 */
@Data
public class AttendanceRateChartVO implements Serializable {

    /** 月份标签列表，如 ["2026-01", "2026-02", ...] */
    private List<String> months;

    /** 各部门出勤率数据 */
    private List<SeriesItem> series;

    /** 单条折线数据 */
    @Data
    public static class SeriesItem implements Serializable {
        /** 部门名称 */
        private String departmentName;
        /** 各月出勤率列表，与 months 下标一一对应 */
        private List<BigDecimal> rates;
        private static final long serialVersionUID = 1L;
    }

    private static final long serialVersionUID = 1L;
}