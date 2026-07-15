package com.limou.hrms.model.vo.salary;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import lombok.Data;

/**
 * 工资条 VO（员工自助查看用）
 */
@Data
public class PayslipVO implements Serializable {

    private Long id;

    private String batchNo;

    private String salaryMonth;

    private String employeeName;

    private String employeeNo;

    private String departmentName;

    /**
     * 收入明细（type=1,2 的项目）
     */
    private List<SalaryItemDetailVO> incomeItems;

    /**
     * 扣除明细（type=3,4,5,6 的项目）
     */
    private List<SalaryItemDetailVO> deductionItems;

    private BigDecimal grossPay;

    private BigDecimal totalDeductions;

    private BigDecimal netPay;

    private Integer payslipViewed;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}
