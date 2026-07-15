package com.limou.hrms.model.vo.salary;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import lombok.Data;

/**
 * 薪资明细 VO（含薪资项目明细列表）
 */
@Data
public class SalaryDetailVO implements Serializable {

    private Long id;

    private Long batchId;

    private Long employeeId;

    /**
     * 工资项目明细列表
     */
    private List<SalaryItemDetailVO> salaryItems;

    private BigDecimal grossPay;

    private BigDecimal socialSecurity;

    private BigDecimal housingFund;

    private BigDecimal incomeTax;

    private BigDecimal totalDeductions;

    private BigDecimal netPay;

    private Integer isAbnormal;

    private String abnormalLabel;

    private String abnormalReason;

    private BigDecimal manualAdjustment;

    private String adjustmentReason;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}
