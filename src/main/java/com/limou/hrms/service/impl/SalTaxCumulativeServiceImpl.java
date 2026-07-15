package com.limou.hrms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.limou.hrms.mapper.SalTaxCumulativeMapper;
import com.limou.hrms.model.entity.SalTaxCumulative;
import com.limou.hrms.service.SalTaxCumulativeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

/**
 * 个税累计服务实现 —— 累计预扣法
 */
@Service
@Slf4j
public class SalTaxCumulativeServiceImpl extends ServiceImpl<SalTaxCumulativeMapper, SalTaxCumulative>
        implements SalTaxCumulativeService {

    /** 每月起征点 */
    private static final BigDecimal MONTHLY_THRESHOLD = BigDecimal.valueOf(5000);

    /**
     * 7级累进税率表: [应纳税所得额上限, 税率, 速算扣除数]
     */
    private static final BigDecimal[][] TAX_BRACKETS = {
            {BigDecimal.valueOf(36000),   BigDecimal.valueOf(0.03), BigDecimal.valueOf(0)},
            {BigDecimal.valueOf(144000),  BigDecimal.valueOf(0.10), BigDecimal.valueOf(2520)},
            {BigDecimal.valueOf(300000),  BigDecimal.valueOf(0.20), BigDecimal.valueOf(16920)},
            {BigDecimal.valueOf(420000),  BigDecimal.valueOf(0.25), BigDecimal.valueOf(31920)},
            {BigDecimal.valueOf(660000),  BigDecimal.valueOf(0.30), BigDecimal.valueOf(52920)},
            {BigDecimal.valueOf(960000),  BigDecimal.valueOf(0.35), BigDecimal.valueOf(85920)},
            {BigDecimal.valueOf(Long.MAX_VALUE), BigDecimal.valueOf(0.45), BigDecimal.valueOf(181920)},
    };

    @Override
    public BigDecimal calculateMonthlyTax(Long employeeId, int taxYear, int taxMonth,
                                          BigDecimal currentGross, BigDecimal currentSS,
                                          BigDecimal currentHF) {
        // 查询上月累计
        SalTaxCumulative lastMonth = getByEmployeeYearMonth(employeeId, taxYear, taxMonth - 1);

        BigDecimal cumulativeGross = currentGross;
        BigDecimal cumulativeSS = currentSS;
        BigDecimal cumulativeHF = currentHF;
        BigDecimal cumulativeSpecial = BigDecimal.ZERO;
        BigDecimal cumulativeTaxPaid = BigDecimal.ZERO;

        if (lastMonth != null) {
            cumulativeGross = lastMonth.getCumulativeGrossPay().add(currentGross);
            cumulativeSS = lastMonth.getCumulativeSocialSecurity().add(currentSS);
            cumulativeHF = lastMonth.getCumulativeHousingFund().add(currentHF);
            cumulativeSpecial = lastMonth.getCumulativeSpecialDeduction();
            cumulativeTaxPaid = lastMonth.getCumulativeTaxPaid();
        }

        // 累计起征点
        BigDecimal cumulativeThreshold = MONTHLY_THRESHOLD.multiply(BigDecimal.valueOf(taxMonth));

        // 累计应纳税所得额 = 累计应发 - 累计起征点 - 累计社保 - 累计公积金 - 累计专项附加扣除
        BigDecimal taxableIncome = cumulativeGross
                .subtract(cumulativeThreshold)
                .subtract(cumulativeSS)
                .subtract(cumulativeHF)
                .subtract(cumulativeSpecial)
                .max(BigDecimal.ZERO);

        // 查税率表
        BigDecimal rate = BigDecimal.ZERO;
        BigDecimal quickDeduction = BigDecimal.ZERO;
        for (BigDecimal[] bracket : TAX_BRACKETS) {
            if (taxableIncome.compareTo(bracket[0]) <= 0) {
                rate = bracket[1];
                quickDeduction = bracket[2];
                break;
            }
        }

        // 累计应缴个税
        BigDecimal cumulativeTaxPayable = taxableIncome.multiply(rate)
                .subtract(quickDeduction)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        // 当月应缴 = 累计应缴 - 累计已缴
        BigDecimal currentMonthTax = cumulativeTaxPayable.subtract(cumulativeTaxPaid)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        // 更新累计已缴 = 累计已缴 + 当月应缴
        BigDecimal newCumulativePaid = cumulativeTaxPaid.add(currentMonthTax);

        // 保存当月累计记录
        SalTaxCumulative record = new SalTaxCumulative();
        record.setEmployeeId(employeeId);
        record.setTaxYear(taxYear);
        record.setTaxMonth(taxMonth);
        record.setCumulativeGrossPay(cumulativeGross.setScale(2, RoundingMode.HALF_UP));
        record.setCumulativeThreshold(cumulativeThreshold.setScale(2, RoundingMode.HALF_UP));
        record.setCumulativeSocialSecurity(cumulativeSS.setScale(2, RoundingMode.HALF_UP));
        record.setCumulativeHousingFund(cumulativeHF.setScale(2, RoundingMode.HALF_UP));
        record.setCumulativeSpecialDeduction(cumulativeSpecial);
        record.setCumulativeTaxableIncome(taxableIncome.setScale(2, RoundingMode.HALF_UP));
        record.setTaxRate(rate.setScale(4, RoundingMode.HALF_UP));
        record.setQuickDeduction(quickDeduction.setScale(2, RoundingMode.HALF_UP));
        record.setCumulativeTaxPayable(cumulativeTaxPayable);
        record.setCumulativeTaxPaid(newCumulativePaid);
        record.setCurrentMonthTax(currentMonthTax);
        record.setCreateTime(new Date());
        this.save(record);

        log.info("个税计算: employeeId={}, {}-{}, 应纳税所得额={}, 税率={}, 当月个税={}",
                employeeId, taxYear, taxMonth, taxableIncome, rate, currentMonthTax);

        return currentMonthTax;
    }

    @Override
    public SalTaxCumulative getByEmployeeYearMonth(Long employeeId, int taxYear, int taxMonth) {
        return this.lambdaQuery()
                .eq(SalTaxCumulative::getEmployeeId, employeeId)
                .eq(SalTaxCumulative::getTaxYear, taxYear)
                .eq(SalTaxCumulative::getTaxMonth, taxMonth)
                .one();
    }
}
