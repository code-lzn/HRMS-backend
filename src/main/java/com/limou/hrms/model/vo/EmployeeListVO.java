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

    @ExcelIgnore
    private Long id;

    @ExcelProperty("工号")
    private String employeeNo;

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("部门")
    private String departmentName;

    @ExcelProperty("职位")
    private String positionName;

    @ExcelIgnore
    private String jobLevel;

    @ExcelIgnore
    private Integer status;

    @ExcelProperty("状态")
    private String statusDesc;

    @ExcelProperty("入职日期")
    private String hireDate;
}
