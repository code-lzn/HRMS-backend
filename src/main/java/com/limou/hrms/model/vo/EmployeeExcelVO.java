package com.limou.hrms.model.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 员工 Excel 导出 VO
 *
 * @author lubo
 */
@Data
public class EmployeeExcelVO implements Serializable {

    @ExcelProperty("工号")
    private String employeeNo;

    @ExcelProperty("姓名")
    private String employeeName;

    @ExcelProperty("性别")
    private String genderDesc;

    @ExcelProperty("手机号")
    private String phone;

    @ExcelProperty("邮箱")
    private String email;

    @ExcelProperty("部门")
    private String departmentName;

    @ExcelProperty("职位")
    private String positionName;

    @ExcelProperty("职级")
    private String jobLevel;

    @ExcelProperty("录用类型")
    private String employmentTypeDesc;

    @ExcelProperty("在职状态")
    private String statusDesc;

    @ExcelProperty("入职日期")
    @DateTimeFormat("yyyy-MM-dd")
    private Date hireDate;

    private static final long serialVersionUID = 1L;
}
