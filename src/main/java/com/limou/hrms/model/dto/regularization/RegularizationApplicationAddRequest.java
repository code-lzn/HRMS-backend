package com.limou.hrms.model.dto.regularization;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 转正申请创建请求
 */
@Data
public class RegularizationApplicationAddRequest implements Serializable {

    private Long employeeId;
    private Date probationStartDate;
    private Date probationEndDate;
    private String performanceReview;
    private BigDecimal salaryAdjustment;
    private Integer result;
    private Date extendedProbationDate;

    private static final long serialVersionUID = 1L;
}
