package com.limou.hrms.service.salary.calculator;

import com.limou.hrms.model.entity.EmployeeSalary;
import com.limou.hrms.model.entity.SalaryItem;
import java.util.List;
import lombok.Data;

/**
 * 薪资计算上下文
 */
@Data
public class SalaryCalculationContext {

    private Long employeeId;

    private String salaryMonth;

    private EmployeeSalary employeeSalary;

    private List<SalaryItem> salaryItems;

    private Integer lateCount;

    private Double leaveDays;

    private Double overtimeHours;

    /**
     * 获取日工资 = 基本工资 / 21.75
     */
    public java.math.BigDecimal getDailySalary() {
        if (employeeSalary == null || employeeSalary.getBaseSalary() == null) {
            return java.math.BigDecimal.ZERO;
        }
        return employeeSalary.getBaseSalary().divide(
                new java.math.BigDecimal("21.75"), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 获取小时工资 = 日工资 / 8
     */
    public java.math.BigDecimal getHourlySalary() {
        return getDailySalary().divide(
                new java.math.BigDecimal("8"), 2, java.math.RoundingMode.HALF_UP);
    }
}
