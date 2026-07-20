package com.limou.hrms.model.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 社保公积金对比数据（分组柱状图）
 */
@Data
public class SalarySocialSecurityVO {

    /** 项目名称：养老保险、医疗保险、失业保险、住房公积金 */
    private String itemName;

    /** 企业缴纳金额 */
    private BigDecimal companyAmount;

    /** 个人缴纳金额 */
    private BigDecimal personalAmount;
}
