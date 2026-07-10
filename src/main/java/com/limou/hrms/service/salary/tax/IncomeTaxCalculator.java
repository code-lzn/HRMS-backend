package com.limou.hrms.service.salary.tax;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.limou.hrms.mapper.IncomeTaxCumulativeMapper;
import com.limou.hrms.model.entity.IncomeTaxCumulative;
import java.math.BigDecimal;
import java.math.RoundingMode;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 个税计算器 — 累计预扣法
 */
@Component
public class IncomeTaxCalculator {

    /**
     * 每月起征点
     */
    private static final BigDecimal MONTHLY_THRESHOLD = new BigDecimal("5000");

    @Resource
    private IncomeTaxCumulativeMapper incomeTaxCumulativeMapper;

    /**
     * 计算当月应缴个税
     *
     * @param employeeId       员工ID
     * @param taxYear          纳税年度
     * @param taxMonth         纳税月份 (1-12)
     * @param currentGrossPay  当月应发工资
     * @param currentSS        当月社保扣除（绝对值）
     * @param currentHF        当月公积金扣除（绝对值）
     * @param currentSpecialDeduction 当月专项附加扣除
     * @return 当月应缴个税
     */
    public IncomeTaxCumulative calculateMonthlyTax(Long employeeId, int taxYear, int taxMonth,
                                                    BigDecimal currentGrossPay, BigDecimal currentSS,
                                                    BigDecimal currentHF, BigDecimal currentSpecialDeduction) {
        // 查询上月累计数据
        IncomeTaxCumulative lastMonth = findLastMonth(employeeId, taxYear, taxMonth);

        BigDecimal cumulativeGrossPay;
        BigDecimal cumulativeSS;
        BigDecimal cumulativeHF;
        BigDecimal cumulativeSpecialDeduction;
        BigDecimal cumulativeTaxPaid;

        if (lastMonth != null) {
            cumulativeGrossPay = lastMonth.getCumulative_gross_pay().add(currentGrossPay);
            cumulativeSS = lastMonth.getCumulative_social_security().add(currentSS);
            cumulativeHF = lastMonth.getCumulative_housing_fund().add(currentHF);
            cumulativeSpecialDeduction = lastMonth.getCumulative_special_deduction().add(currentSpecialDeduction);
            cumulativeTaxPaid = lastMonth.getCumulative_tax_paid();
        } else {
            cumulativeGrossPay = currentGrossPay;
            cumulativeSS = currentSS;
            cumulativeHF = currentHF;
            cumulativeSpecialDeduction = currentSpecialDeduction;
            cumulativeTaxPaid = BigDecimal.ZERO;
        }

        // 累计起征点
        BigDecimal cumulativeThreshold = MONTHLY_THRESHOLD.multiply(new BigDecimal(taxMonth));

        // 累计应纳税所得额
        BigDecimal cumulativeTaxableIncome = cumulativeGrossPay
                .subtract(cumulativeThreshold)
                .subtract(cumulativeSS)
                .subtract(cumulativeHF)
                .subtract(cumulativeSpecialDeduction)
                .max(BigDecimal.ZERO);

        // 查找税率区间
        TaxBracket bracket = TaxBracket.findBracket(cumulativeTaxableIncome);

        // 累计应缴个税 = 累计应纳税所得额 × 税率 - 速算扣除数
        BigDecimal cumulativeTaxPayable = cumulativeTaxableIncome
                .multiply(bracket.getRate())
                .subtract(bracket.getQuickDeduction())
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        // 当月应缴个税 = 累计应缴 - 累计已缴
        BigDecimal currentMonthTax = cumulativeTaxPayable.subtract(cumulativeTaxPaid).max(BigDecimal.ZERO);

        // 构建累计记录
        IncomeTaxCumulative record = new IncomeTaxCumulative();
        record.setEmployee_id(employeeId);
        record.setTax_year(taxYear);
        record.setTax_month(taxMonth);
        record.setCumulative_gross_pay(cumulativeGrossPay);
        record.setCumulative_threshold(cumulativeThreshold);
        record.setCumulative_social_security(cumulativeSS);
        record.setCumulative_housing_fund(cumulativeHF);
        record.setCumulative_special_deduction(cumulativeSpecialDeduction);
        record.setCumulative_taxable_income(cumulativeTaxableIncome);
        record.setTax_rate(bracket.getRate());
        record.setQuick_deduction(bracket.getQuickDeduction());
        record.setCumulative_tax_payable(cumulativeTaxPayable);
        record.setCumulative_tax_paid(cumulativeTaxPaid.add(currentMonthTax));
        record.setCurrent_month_tax(currentMonthTax);

        return record;
    }

    /**
     * 查询上月的累计记录
     */
    private IncomeTaxCumulative findLastMonth(Long employeeId, int taxYear, int taxMonth) {
        if (taxMonth <= 1) {
            return null;
        }
        QueryWrapper<IncomeTaxCumulative> wrapper = new QueryWrapper<>();
        wrapper.eq("employee_id", employeeId)
                .eq("tax_year", taxYear)
                .eq("tax_month", taxMonth - 1);
        return incomeTaxCumulativeMapper.selectOne(wrapper);
    }
}
