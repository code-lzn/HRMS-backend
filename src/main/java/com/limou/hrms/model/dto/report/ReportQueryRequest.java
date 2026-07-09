package com.limou.hrms.model.dto.report;

import com.limou.hrms.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class ReportQueryRequest extends PageRequest implements Serializable {
    private String projectName;

    private String constructionUnit;

    private String culturalUnitName;

    private String culturalUnitLevel;

    private String projectType;

    private BigDecimal budgetAmount;

    private BigDecimal approvedAmount;

    private String approvalNumber;

    private Date startDate;

    private Date endDate;
    //备注
    private String remarks;

    private static final long serialVersionUID = 1L;

}