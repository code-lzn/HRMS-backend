package com.limou.hrms.model.vo;

import lombok.Data;

/**
 * 薪资变动分布数据（直方图）
 */
@Data
public class SalaryChangeDistributionVO {

    /** 变动区间标签，如 "-30%以下", "-10%~0%", "+10%~30%" 等 */
    private String rangeLabel;

    /** 该区间员工数量 */
    private Integer count;
}
