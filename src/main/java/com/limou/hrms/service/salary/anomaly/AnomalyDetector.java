package com.limou.hrms.service.salary.anomaly;

import com.limou.hrms.model.enums.AbnormalLevelEnum;
import com.limou.hrms.service.salary.calculator.SalaryCalculationContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 异常检测规则引擎
 */
@Component
public class AnomalyDetector {

    private static final int LEAVE_WARNING_DAYS = 15;
    private static final int OVERTIME_WARNING_HOURS = 50;
    private static final BigDecimal CHANGE_WARNING_RATE = new BigDecimal("0.30");

    /**
     * 执行异常检测
     *
     * @param ctx 计算上下文
     * @return 异常检测结果列表
     */
    public List<AnomalyResult> detect(SalaryCalculationContext ctx) {
        List<AnomalyResult> anomalies = new ArrayList<>();

        // 黄色预警：请假 > 15天
        if (ctx.getLeaveDays() > LEAVE_WARNING_DAYS) {
            anomalies.add(new AnomalyResult(
                    AbnormalLevelEnum.WARNING,
                    "当月请假超过" + LEAVE_WARNING_DAYS + "天（当前" + ctx.getLeaveDays() + "天）"
            ));
        }

        // 黄色预警：加班 > 50小时
        if (ctx.getOvertimeHours() > OVERTIME_WARNING_HOURS) {
            anomalies.add(new AnomalyResult(
                    AbnormalLevelEnum.WARNING,
                    "当月加班超过" + OVERTIME_WARNING_HOURS + "小时（当前" + ctx.getOvertimeHours() + "小时）"
            ));
        }

        // 红色预警：变动 > 30%（需要上月数据）
        if (ctx.getLastMonthNetPay() != null && ctx.getLastMonthNetPay().compareTo(BigDecimal.ZERO) > 0) {
            // 这里标记需要比对，计算在外部完成
            anomalies.add(new AnomalyResult(
                    AbnormalLevelEnum.NORMAL,
                    "NEED_COMPARISON"
            ));
        }

        return anomalies;
    }

    /**
     * 检测薪资变动是否超过30%
     */
    public AnomalyResult checkSalaryChange(BigDecimal currentNetPay, BigDecimal lastMonthNetPay) {
        if (lastMonthNetPay == null || lastMonthNetPay.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal change = currentNetPay.subtract(lastMonthNetPay)
                .divide(lastMonthNetPay, 2, RoundingMode.HALF_UP).abs();
        if (change.compareTo(CHANGE_WARNING_RATE) > 0) {
            return new AnomalyResult(
                    AbnormalLevelEnum.ERROR,
                    "薪资较上月变动超过30%（变动率：" + change.multiply(new BigDecimal("100")) + "%）"
            );
        }
        return null;
    }

    /**
     * 检查新员工是否有薪资档案
     */
    public AnomalyResult checkNewEmployeeHasNoSalary(Long employeeId) {
        // 由业务层在数据准备阶段处理
        return new AnomalyResult(
                AbnormalLevelEnum.BLOCKED,
                "新员工(ID:" + employeeId + ")无薪资档案，无法计算"
        );
    }
}
