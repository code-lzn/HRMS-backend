package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 工资条列表 VO
 */
@Data
public class PayslipListVO implements Serializable {

    private Long id;

    /**
     * 薪资月份 yyyy-MM
     */
    private String yearMonth;

    /**
     * 实发工资
     */
    private BigDecimal netSalary;

    /**
     * 状态：4=已通过 5=已发放
     */
    private Integer status;

    private String statusDesc;

    /**
     * 是否已查看
     */
    private Boolean hasViewed;

    private static final long serialVersionUID = 1L;
}
