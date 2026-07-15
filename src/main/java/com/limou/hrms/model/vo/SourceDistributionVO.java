package com.limou.hrms.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 数据分析 - 用户来源分布
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SourceDistributionVO implements Serializable {

    /** 来源名称 */
    private String sourceName;
    /** 数量 */
    private Long count;
    /** 占比(%) */
    private BigDecimal percentage;

    private static final long serialVersionUID = 1L;
}
