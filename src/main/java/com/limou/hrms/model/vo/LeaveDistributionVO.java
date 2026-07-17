package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class LeaveDistributionVO implements Serializable {

    private String leaveTypeDesc;

    private BigDecimal days;

    private BigDecimal percentage;

    private static final long serialVersionUID = 1L;
}