package com.limou.hrms.model.vo.salary;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 工资条中单个项目的明细 VO
 */
@Data
public class SalaryItemDetailVO implements Serializable {

    private String name;

    private BigDecimal amount;

    private Integer type;

    private static final long serialVersionUID = 1L;
}
