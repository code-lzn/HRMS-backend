package com.limou.hrms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.limou.hrms.model.entity.SalTaxCumulative;

import java.math.BigDecimal;

/**
 * 个税累计服务
 */
public interface SalTaxCumulativeService extends IService<SalTaxCumulative> {

    /**
     * 计算当月个税（累计预扣法）
     *
     * @param employeeId     员工ID
     * @param taxYear        纳税年度
     * @param taxMonth       纳税月份
     * @param currentGross   当月应发工资
     * @param currentSS      当月社保扣除
     * @param currentHF      当月公积金扣除
     * @return 当月应缴个税
     */
    BigDecimal calculateMonthlyTax(Long employeeId, int taxYear, int taxMonth,
                                   BigDecimal currentGross, BigDecimal currentSS,
                                   BigDecimal currentHF);

    /**
     * 查询员工某月的个税记录
     */
    SalTaxCumulative getByEmployeeYearMonth(Long employeeId, int taxYear, int taxMonth);
}
