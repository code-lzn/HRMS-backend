package com.limou.hrms.model.dto.report;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class ReportAddRequest implements Serializable {


    private String projectName;


    private String constructionUnit;


    private String userAvatar;

    private String culturalUnitName;


    private String culturalUnitLevel;


    private String projectType;

    private BigDecimal budgetAmount;

    private BigDecimal approvedAmount;

    private String approvalNumber;

    private Date createTime;
    //备注
    private String remarks;
    private static final long serialVersionUID = 1L;
}