package com.limou.hrms.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 数据分析 - 各渠道转化率
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversionRateVO implements Serializable {

    /** 渠道名称 */
    private String channelName;
    /** 曝光量 */
    private Long exposure;
    /** 转化量 */
    private Long conversion;
    /** 转化率(%) */
    private BigDecimal rate;

    private static final long serialVersionUID = 1L;
}
