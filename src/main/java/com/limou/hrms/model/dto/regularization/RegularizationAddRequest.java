package com.limou.hrms.model.dto.regularization;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class RegularizationAddRequest implements Serializable {

    private Long employeeId;

    private String evaluation;

    private BigDecimal probationScore;

    private String result;

    private Integer extendedMonths;

    private BigDecimal salaryAdjustment;

    private String adjustRemark;

    private Long flowId;

    private String remark;

    private static final long serialVersionUID = 1L;
}
