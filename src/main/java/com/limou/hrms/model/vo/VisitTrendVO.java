package com.limou.hrms.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 工作台 - 近7天访问趋势
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisitTrendVO implements Serializable {

    /** 日期 (yyyy-MM-dd) */
    private String date;
    /** 页面访问量 */
    private Long pageViews;

    private static final long serialVersionUID = 1L;
}
