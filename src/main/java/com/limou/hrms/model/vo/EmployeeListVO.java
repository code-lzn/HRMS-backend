package com.limou.hrms.model.vo;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 员工列表响应（脱敏后，仅展示默认字段）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeListVO {

    /** 员工ID（不导出） */
    @ExcelIgnore
    private Long id;

    /** 工号 */
    @ExcelProperty("工号")
    private String employeeNo;

    /** 姓名 */
    @ExcelProperty("姓名")
    private String name;

    /** 部门名称 */
    @ExcelProperty("部门")
    private String departmentName;

    /** 职位名称 */
    @ExcelProperty("职位")
    private String positionName;

    /** 职级 */
    @ExcelIgnore
    private String jobLevel;

    /** 在职状态码（不导出） */
    @ExcelIgnore
    private Integer status;

    /** 在职状态描述 */
    @ExcelProperty("状态")
    private String statusDesc;

    /** 入职日期 */
    @ExcelProperty("入职日期")
    private String hireDate;

    /** 直接汇报人姓名 */
    @ExcelProperty("直接汇报人")
    private String directReportName;
}