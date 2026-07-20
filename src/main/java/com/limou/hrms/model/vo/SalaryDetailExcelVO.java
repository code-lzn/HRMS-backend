package com.limou.hrms.model.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.NumberFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 薪资核算明细 Excel 导出 VO
 *
 * @author lubo
 */
@Data
public class SalaryDetailExcelVO implements Serializable {

    @ExcelProperty("工号")
    @ColumnWidth(14)
    private String employeeNo;

    @ExcelProperty("姓名")
    @ColumnWidth(12)
    private String employeeName;

    @ExcelProperty("部门")
    @ColumnWidth(14)
    private String departmentName;

    // ========== 收入项 ==========

    @ExcelProperty("基本工资")
    @NumberFormat("#,##0.00")
    @ColumnWidth(12)
    private BigDecimal baseSalary;

    @ExcelProperty("岗位津贴")
    @NumberFormat("#,##0.00")
    @ColumnWidth(12)
    private BigDecimal allowance;

    @ExcelProperty("绩效奖金")
    @NumberFormat("#,##0.00")
    @ColumnWidth(12)
    private BigDecimal performanceBonus;

    @ExcelProperty("加班费")
    @NumberFormat("#,##0.00")
    @ColumnWidth(12)
    private BigDecimal overtimePay;

    // ========== 扣除项 ==========

    @ExcelProperty("迟到扣款")
    @NumberFormat("#,##0.00")
    @ColumnWidth(12)
    private BigDecimal lateDeduction;

    @ExcelProperty("请假扣款")
    @NumberFormat("#,##0.00")
    @ColumnWidth(12)
    private BigDecimal leaveDeduction;

    @ExcelProperty("养老保险")
    @NumberFormat("#,##0.00")
    @ColumnWidth(12)
    private BigDecimal socialPension;

    @ExcelProperty("医疗保险")
    @NumberFormat("#,##0.00")
    @ColumnWidth(12)
    private BigDecimal socialMedical;

    @ExcelProperty("失业保险")
    @NumberFormat("#,##0.00")
    @ColumnWidth(12)
    private BigDecimal socialUnemployment;

    @ExcelProperty("住房公积金")
    @NumberFormat("#,##0.00")
    @ColumnWidth(12)
    private BigDecimal housingFund;

    @ExcelProperty("个人所得税")
    @NumberFormat("#,##0.00")
    @ColumnWidth(12)
    private BigDecimal incomeTax;

    // ========== 汇总 ==========

    @ExcelProperty("应发工资")
    @NumberFormat("#,##0.00")
    @ColumnWidth(12)
    private BigDecimal grossSalary;

    @ExcelProperty("应扣合计")
    @NumberFormat("#,##0.00")
    @ColumnWidth(12)
    private BigDecimal totalDeduction;

    @ExcelProperty("实发工资")
    @NumberFormat("#,##0.00")
    @ColumnWidth(12)
    private BigDecimal netSalary;

    // ========== 调整 ==========

    @ExcelProperty("手动调整")
    @NumberFormat("#,##0.00")
    @ColumnWidth(12)
    private BigDecimal manualAdjust;

    @ExcelProperty("调整原因")
    @ColumnWidth(20)
    private String adjustReason;

    // ========== 异常 ==========

    @ExcelProperty("是否异常")
    @ColumnWidth(10)
    private String anomalyDesc;

    @ExcelProperty("异常说明")
    @ColumnWidth(24)
    private String anomalyReason;

    private static final long serialVersionUID = 1L;
}
