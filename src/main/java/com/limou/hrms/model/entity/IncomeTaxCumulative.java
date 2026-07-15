package com.limou.hrms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 个税累计表
 */
@TableName(value = "income_tax_cumulative")
@Data
public class IncomeTaxCumulative implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 员工 ID → user.id
     */
    private Long employeeId;

    /**
     * 纳税年度，如 2026
     */
    private Integer taxYear;

    /**
     * 纳税月份，1~12
     */
    private Integer taxMonth;

    /**
     * 累计应发工资
     */
    private BigDecimal cumulativeGrossPay;

    /**
     * 累计起征点 = 5000 × 月数
     */
    private BigDecimal cumulativeThreshold;

    /**
     * 累计社保个人部分
     */
    private BigDecimal cumulativeSocialSecurity;

    /**
     * 累计公积金个人部分
     */
    private BigDecimal cumulativeHousingFund;

    /**
     * 累计专项附加扣除
     */
    private BigDecimal cumulativeSpecialDeduction;

    /**
     * 累计应纳税所得额
     */
    private BigDecimal cumulativeTaxableIncome;

    /**
     * 适用税率
     */
    private BigDecimal taxRate;

    /**
     * 速算扣除数
     */
    private BigDecimal quickDeduction;

    /**
     * 累计应纳税额
     */
    private BigDecimal cumulativeTaxPayable;

    /**
     * 累计已缴税额
     */
    private BigDecimal cumulativeTaxPaid;

    /**
     * 本月应缴个税
     */
    private BigDecimal currentMonthTax;

    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
