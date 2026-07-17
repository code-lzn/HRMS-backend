package com.limou.hrms.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 假期余额 VO
 */
@Data
public class LeaveBalanceVO implements Serializable {

    private Long employeeId;

    private List<BalanceItem> balances;

    @Data
    public static class BalanceItem implements Serializable {
        private Integer leaveType;
        private String leaveTypeDesc;
        private BigDecimal totalDays;
        private BigDecimal usedDays;
        private BigDecimal remainingDays;
        private static final long serialVersionUID = 1L;
    }

    private static final long serialVersionUID = 1L;
}
