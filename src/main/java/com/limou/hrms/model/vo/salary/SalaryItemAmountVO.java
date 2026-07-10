package com.limou.hrms.model.vo.salary;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 工资项金额VO（预览表格每个工资项的金额）
 */
@Data
public class SalaryItemAmountVO {

    private String name;

    private Integer type;

    private BigDecimal amount;
}
