package com.limou.hrms.model.vo.salary;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 薪资构成占比VO（AntV饼图）
 */
@Data
public class CompositionVO {

    private String item_name;

    private BigDecimal amount;

    private BigDecimal percentage;
}
