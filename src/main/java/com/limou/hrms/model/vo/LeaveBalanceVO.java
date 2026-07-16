package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class LeaveBalanceVO implements Serializable {

    /** 年假剩余 */
    private BigDecimal annualRemaining;

    /** 病假剩余 */
    private BigDecimal sickRemaining;

    /** 调休剩余 */
    private BigDecimal compRemaining;

    /** 总剩余 */
    private BigDecimal totalRemaining;

    private static final long serialVersionUID = 1L;
}
