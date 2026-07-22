package com.limou.hrms.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 员工薪资与合同信息 VO（脱敏后，仅 HR/财务可见）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryInfoVO {

    /** 合同类型：1=固定期限 2=无固定期限 3=实习协议 */
    private Integer contractType;

    /** 合同类型描述 */
    private String contractTypeDesc;

    /** 合同到期日 */
    private String contractExpireDate;

    /** 试用期待遇比例（如 0.8） */
    private BigDecimal probationRatio;

    /** 薪资账套ID */
    private Long salaryAccountId;

    /** 薪资账套名称 */
    private String salaryAccountName;

    /** 基本工资 */
    private BigDecimal baseSalary;

    /** 银行卡号（按角色脱敏） */
    private String bankAccount;

    /** 开户行名称 */
    private String bankName;
}